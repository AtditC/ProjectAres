package tc.oc.pgm.polls.types;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import tc.oc.api.docs.PlayerId;
import tc.oc.commons.bukkit.chat.PlayerComponent;
import tc.oc.commons.bukkit.nick.IdentityProvider;
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

    @AssistedInject PollKick(@Assisted PlayerId initiator, @Assisted Player player, PollManager pollManager, Audiences audiences, IdentityProvider identityProvider) {
        super(pollManager, initiator, audiences);
        this.player = player;
        this.identityProvider = identityProvider;
    }

    @Override
    public void executeAction() {
        if(player != null) player.kickPlayer(ChatColor.DARK_RED + "You were poll-kicked.");
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
