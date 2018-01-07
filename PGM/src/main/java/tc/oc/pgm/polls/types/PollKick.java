package tc.oc.pgm.polls.types;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.api.docs.PlayerId;
import tc.oc.api.docs.virtual.PunishmentDoc;
import tc.oc.api.minecraft.users.UserStore;
import tc.oc.commons.bukkit.chat.PlayerComponent;
import tc.oc.commons.bukkit.nick.IdentityProvider;
import tc.oc.commons.bukkit.punishment.PunishmentCreator;
import tc.oc.commons.core.chat.Audiences;
import tc.oc.commons.core.chat.Component;
import tc.oc.pgm.polls.Poll;
import tc.oc.pgm.polls.PollManager;

public class PollKick extends Poll {

    public interface Factory {
        PollKick create(PlayerId initiator, Player player);
    }

    private final Player player;
    private final IdentityProvider identityProvider;
    private final PunishmentCreator punishmentCreator;
    private final UserStore userStore;

    @AssistedInject PollKick(@Assisted PlayerId initiator, @Assisted Player player, PollManager pollManager, Audiences audiences, IdentityProvider identityProvider, PunishmentCreator punishmentCreator, UserStore userStore) {
        super(pollManager, initiator, audiences);
        this.player = player;
        this.identityProvider = identityProvider;
        this.punishmentCreator = punishmentCreator;
        this.userStore = userStore;
    }

    @Override
    public void executeAction() {
        PlayerId kicked = userStore.playerId(player);
        punishmentCreator.create(null, kicked, "The poll to kick " + kicked.username() + " succeeded", PunishmentDoc.Type.KICK, null, false, false, true);
        audiences.localServer().sendMessage(new Component(ChatColor.DARK_AQUA).translate("poll.kick.success", new PlayerComponent(identityProvider.currentIdentity(player))));
    }

    @Override
    public String getActionString() {
        return normalize + "Kick: " + boldAqua + this.player;
    }

    @Override
    public String getDescriptionMessage() {
        return "to kick " + boldAqua +  this.player;
    }
}
