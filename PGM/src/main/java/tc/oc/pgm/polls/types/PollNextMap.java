package tc.oc.pgm.polls.types;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.api.docs.User;
import tc.oc.commons.bukkit.tokens.TokenUtil;
import tc.oc.commons.core.chat.Audiences;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.match.MatchManager;
import tc.oc.pgm.polls.Poll;
import tc.oc.pgm.polls.PollManager;

public class PollNextMap extends Poll {

    public interface Factory {
        PollNextMap create(CommandSender sender, PGMMap nextMap);
    }

    private final MatchManager mm;
    private final PGMMap nextMap;
    private User user;

    @AssistedInject PollNextMap(@Assisted CommandSender sender, @Assisted PGMMap nextMap, PollManager pollManager, Audiences audiences, MatchManager mm) {
        super(pollManager, sender.getName(), audiences);
        this.mm = mm;
        this.nextMap = nextMap;
        if (sender instanceof Player) {
            user = TokenUtil.getUser((Player)sender);
        }
    }

    @Override
    public void executeAction() {
        this.mm.setNextMap(this.nextMap);
        if (user != null) {
            TokenUtil.giveMapTokens(user, -1);
        }
    }

    @Override
    public String getActionString() {
        return normalize + "Next map: " + boldAqua + this.nextMap.getInfo().name;
    }

    @Override
    public String getDescriptionMessage() {
        return "to set the next map to " + boldAqua + this.nextMap.getInfo().name;
    }
}
