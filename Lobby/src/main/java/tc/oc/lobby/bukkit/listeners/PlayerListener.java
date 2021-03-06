package tc.oc.lobby.bukkit.listeners;

import java.util.Random;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.google.common.eventbus.Subscribe;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventBus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInitialSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.geometry.Cuboid;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.time.Instant;
import tc.oc.api.bukkit.users.OnlinePlayers;
import tc.oc.api.docs.User;
import tc.oc.api.docs.virtual.ServerDoc;
import tc.oc.api.minecraft.MinecraftService;
import tc.oc.api.util.Permissions;
import tc.oc.commons.bukkit.chat.Audiences;
import tc.oc.commons.bukkit.chat.ComponentRenderContext;
import tc.oc.commons.bukkit.chat.HeaderComponent;
import tc.oc.commons.bukkit.event.ObserverKitApplyEvent;
import tc.oc.commons.bukkit.event.UserLoginEvent;
import tc.oc.commons.core.chat.Audience;
import tc.oc.commons.core.chat.Component;
import tc.oc.commons.core.chat.Components;
import tc.oc.commons.core.format.GeneralFormatter;
import tc.oc.commons.core.plugin.PluginFacet;
import tc.oc.commons.core.restart.RequestRestartEvent;
import tc.oc.commons.core.util.TimeUtils;
import tc.oc.lobby.bukkit.LobbyConfig;
import tc.oc.lobby.bukkit.Utils;
import tc.oc.commons.bukkit.chat.Links;

import static net.md_5.bungee.api.ChatColor.*;

public class PlayerListener implements PluginFacet, Listener {
    class SignUpdate implements ServerDoc.StatusUpdate {
        final int num_observing;

        SignUpdate(int num_observing) {
            this.num_observing = num_observing;
        }

        @Override
        public int num_observing() {
            return num_observing;
        }

        @Override
        public int max_players() {
            return lobby.getServer().getMaxPlayers();
        }
    }

    private final Plugin lobby;
    private final LobbyConfig config;
    private final MinecraftService minecraftService;
    private final OnlinePlayers onlinePlayers;
    private final Audiences audiences;
    private final ComponentRenderContext renderer;
    private final EventBus eventBus;
    private final GeneralFormatter generalFormatter;

    private Cuboid boundaries = Cuboid.unbounded();
    private Location spawnLocation;
    private double spawnRadius;
    private @Nullable RequestRestartEvent.Deferral restartDeferral;

    @Inject PlayerListener(Plugin lobby, LobbyConfig config, MinecraftService minecraftService, OnlinePlayers onlinePlayers, Audiences audiences,  ComponentRenderContext renderer, EventBus eventBus, GeneralFormatter generalFormatter) {
        this.lobby = lobby;
        this.config = config;
        this.minecraftService = minecraftService;
        this.onlinePlayers = onlinePlayers;
        this.audiences = audiences;
        this.renderer = renderer;
        this.eventBus = eventBus;
        this.generalFormatter = generalFormatter;
    }

    private Location spawnLocation(World world) {
        try {
            return spawnLocation = config.getSpawnLocation(world);
        } catch(Throwable e) {
            lobby.getLogger().log(Level.SEVERE, "Failed to get spawn location, using last good value", e);
            return spawnLocation;
        }
    }

    private double spawnRadius(World world) {
        try {
            return spawnRadius = config.getSpawnRadius(world);
        } catch(Throwable e) {
            lobby.getLogger().log(Level.SEVERE, "Failed to get spawn radius, using last good value", e);
            return spawnRadius;
        }
    }

    private Cuboid boundaries(World world) {
        try {
            return boundaries = config.getBoundaries(world);
        } catch(Throwable e) {
            lobby.getLogger().log(Level.SEVERE, "Failed to get boundaries, using last good value", e);
            return boundaries;
        }
    }

    private void resetPlayer(Player player) {
        Utils.resetPlayer(player);
        eventBus.callEvent(new ObserverKitApplyEvent(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void join(final PlayerJoinEvent event) {
        Player player = event.getPlayer();

        resetPlayer(player);

        event.getPlayer().addAttachment(lobby, Permissions.OBSERVER, true);

        if(!player.hasPermission("lobby.disabled-permissions-exempt")) {
            for(PermissionAttachmentInfo attachment : player.getEffectivePermissions()) {
                if(config.getDisabledPermissions().contains(attachment.getPermission())) {
                    attachment.getAttachment().setPermission(attachment.getPermission(), false);
                }
            }
        }

        int count = lobby.getServer().getOnlinePlayers().size();
        if(!lobby.getServer().getOnlinePlayers().contains(event.getPlayer())) count++;
        minecraftService.updateLocalServer(new SignUpdate(count));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLogin(UserLoginEvent event) {
        final User user = event.getUser();
        final Player player = event.getPlayer();
        final Audience audience = audiences.get(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void respawn(final PlayerRespawnEvent event) {
        resetPlayer(event.getPlayer());
    }

    @Subscribe
    public void onRestart(RequestRestartEvent event) {
        if(event.priority() < ServerDoc.Restart.Priority.HIGH && onlinePlayers.count() > 0) {
            restartDeferral = event.defer(getClass().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void leave(final PlayerQuitEvent event) {
        int count = lobby.getServer().getOnlinePlayers().size();
        if(lobby.getServer().getOnlinePlayers().contains(event.getPlayer())) count--;
        minecraftService.updateLocalServer(new SignUpdate(count));

        if(count == 0 && restartDeferral != null) {
            final RequestRestartEvent.Deferral deferral = restartDeferral;
            restartDeferral = null;
            deferral.resume();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void spawn(final PlayerInitialSpawnEvent event) {
        Location location = spawnLocation(event.getWorld());
        double radius = spawnRadius(event.getWorld());
        Random random = new Random();

        // Random point in circle
        double angle = random.nextDouble() * Math.PI * 2;
        double hyp = random.nextDouble() + random.nextDouble();
        hyp = (hyp < 1D ? hyp : 2 - hyp) * radius;
        location.setX(Math.cos(angle) * hyp + location.getX());
        location.setZ(Math.sin(angle) * hyp + location.getZ());

        event.setSpawnLocation(location);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void command(final PlayerCommandPreprocessEvent event) {
        if(event.getMessage().trim().contains("/kill")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void move(final PlayerMoveEvent event) {
        final Cuboid bounds = boundaries(event.getWorld());
        if(!bounds.contains(event.getTo().toVector())) {
            event.getPlayer().teleport(spawnLocation(event.getWorld()));
        }
    }
}
