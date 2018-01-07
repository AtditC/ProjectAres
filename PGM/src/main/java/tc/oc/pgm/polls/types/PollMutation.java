package tc.oc.pgm.polls.types;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.api.docs.User;
import tc.oc.commons.bukkit.tokens.TokenUtil;
import tc.oc.commons.core.chat.Audiences;
import tc.oc.pgm.mutation.Mutation;
import tc.oc.pgm.polls.Poll;
import tc.oc.pgm.polls.PollManager;

public class PollMutation extends Poll {

    public interface Factory {
        PollMutation create(CommandSender sender, Mutation mutation);
    }

    private Mutation mutation;
    private User user;

    @AssistedInject PollMutation(@Assisted CommandSender sender, @Assisted Mutation mutation, PollManager pollManager, Audiences audiences) {
        super(pollManager, sender.getName(), audiences);
        this.mutation = mutation;
        if (sender instanceof Player) {
            user = TokenUtil.getUser((Player)sender);
        }
    }

    @Override
    public void executeAction() {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                "mutation enable -q " + mutation.name().toLowerCase());

        if (user != null) {
            TokenUtil.giveMutationTokens(user, -1);
        }
    }

    @Override
    public String getActionString() {
        return normalize + "Add mutation: " + boldAqua + mutation.name().substring(0,1)
                + mutation.name().toLowerCase().substring(1);
    }

    @Override
    public String getDescriptionMessage() {
        return "to add the " + boldAqua + mutation.name().substring(0,1)
                + mutation.name().toLowerCase().substring(1) + " mutation";
    }
}
