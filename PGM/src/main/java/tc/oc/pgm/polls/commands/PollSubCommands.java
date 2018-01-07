package tc.oc.pgm.polls.commands;

import com.google.common.collect.Sets;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.commons.bukkit.tokens.TokenUtil;
import tc.oc.commons.core.commands.TranslatableCommandException;
import tc.oc.commons.core.formatting.StringUtils;
import tc.oc.commons.core.restart.RestartManager;
import tc.oc.pgm.Config;
import tc.oc.pgm.PGM;
import tc.oc.pgm.commands.CommandUtils;
import tc.oc.pgm.map.PGMMap;
import tc.oc.pgm.mutation.Mutation;
import tc.oc.pgm.mutation.MutationQueue;
import tc.oc.pgm.mutation.command.MutationCommands;
import tc.oc.pgm.polls.PollBlacklist;
import tc.oc.pgm.polls.types.PollCustom;
import tc.oc.pgm.polls.types.PollKick;
import tc.oc.pgm.polls.PollManager;
import tc.oc.pgm.polls.types.PollMutation;
import tc.oc.pgm.polls.types.PollNextMap;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class PollSubCommands {

    private final RestartManager restartManager;
    private final MutationQueue mutationQueue;
    private final PollManager pollManager;
    private final PollCustom.Factory pollCustomFactory;
    private final PollNextMap.Factory pollMapFactory;
    private final PollMutation.Factory pollMutationFactory;
    private final PollKick.Factory pollKickFactory;
    private final PollBlacklist pollBlacklist;

    @Inject
    PollSubCommands(RestartManager restartManager,
                 MutationQueue mutationQueue,
                 PollManager pollManager,
                 PollCustom.Factory pollCustomFactory,
                 PollNextMap.Factory pollMapFactory,
                 PollMutation.Factory pollMutationFactory,
                 PollKick.Factory pollKickFactory,
                 PollBlacklist pollBlacklist) {
        this.restartManager = restartManager;
        this.mutationQueue = mutationQueue;
        this.pollManager = pollManager;
        this.pollCustomFactory = pollCustomFactory;
        this.pollMapFactory = pollMapFactory;
        this.pollMutationFactory = pollMutationFactory;
        this.pollKickFactory = pollKickFactory;
        this.pollBlacklist = pollBlacklist;
    }



    @Command(
        aliases = {"kick"},
        desc = "Start a poll to kick another player.",
        usage = "[player]",
        min = 1,
        max = 1
    )
    @CommandPermissions("poll.kick")
    public void pollKick(CommandContext args, CommandSender sender) throws CommandException {
        Player initiator = tc.oc.commons.bukkit.commands.CommandUtils.senderToPlayer(sender);
        Player player = tc.oc.commons.bukkit.commands.CommandUtils.findOnlinePlayer(args, sender, 0);

        if(player.hasPermission("pgm.poll.kick.exempt") && !initiator.hasPermission("pgm.poll.kick.override")) {
            throw new TranslatableCommandException("poll.kick.exempt");
        } else {
            pollManager.startPoll(pollKickFactory.create(initiator.getName(), player));
        }
    }

    @Command(
        aliases = {"next"},
        desc = "Start a poll to change the next map.",
        usage = "[map name]",
        min = 1,
        max = -1
    )
    @CommandPermissions("poll.next")
    public List<String> pollNext(CommandContext args, CommandSender sender) throws CommandException {
        final String mapName = args.argsLength() > 0 ? args.getJoinedStrings(0) : "";
        if(args.getSuggestionContext() != null) {
            return CommandUtils.completeMapName(mapName);
        }

        if (!Config.Poll.enabled()) {
            throw new TranslatableCommandException("poll.disabled");
        }

        if (restartManager.isRestartRequested()) {
            throw new TranslatableCommandException("poll.map.restarting");
        }
        if (PGM.getMatchManager().hasMapSet()) {
            throw new TranslatableCommandException("poll.map.alreadyset");
        }
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (TokenUtil.getUser(player).maptokens() < 1) {
                throw new TranslatableCommandException("tokens.map.fail");
            }
        }

        PGMMap nextMap = CommandUtils.getMap(args.getJoinedStrings(0), sender);

        if (pollBlacklist.isBlacklisted(nextMap) && !sender.hasPermission("poll.next.override")) {
            throw new TranslatableCommandException("poll.map.notallowed");
        }

        if (PGM.get().getServer().getOnlinePlayers().size() * 4 / 5 > nextMap.getDocument().max_players() && !sender.hasPermission("poll.next.override")) {
            throw new TranslatableCommandException("poll.map.toomanyplayers");
        }

        pollManager.startPoll(pollMapFactory.create(sender, nextMap));
        return null;
    }

    @Command(
            aliases = {"mutation", "mt"},
            desc = "Start a poll to set a mutation",
            usage = "[mutation name]",
            min = 1,
            max = -1
    )
    @CommandPermissions("poll.mutation")
    public List<String> pollMutation(CommandContext args, CommandSender sender) throws CommandException {

        if (args.getSuggestionContext() != null) {
            Collection<Mutation> availableMutations = Sets.newHashSet(Mutation.values());
            availableMutations.removeAll(mutationQueue.mutations());
            return StringUtils.complete(args.getSuggestionContext().getPrefix(), availableMutations.stream().map(mutation -> mutation.name().toLowerCase()));
        }

        if (!Config.Poll.enabled()) {
            throw new TranslatableCommandException("poll.disabled");
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (TokenUtil.getUser(player).mutationtokens() < 1) {
                throw new TranslatableCommandException("tokens.mutation.fail");
            }
        }

        String mutationString = args.getString(0);

        Mutation mutation = StringUtils.bestFuzzyMatch(mutationString, Sets.newHashSet(Mutation.values()), 0.9);
        if(mutation == null) {
            throw new TranslatableCommandException("command.mutation.error.find", mutationString);
        } else if(MutationCommands.getInstance().getMutationQueue().mutations().contains(mutation)) {
            throw new TranslatableCommandException(true ? "command.mutation.error.enabled" : "command.mutation.error.disabled", mutation.getComponent(net.md_5.bungee.api.ChatColor.RED));
        } else if (!mutation.isPollable() && !sender.hasPermission("poll.mutation.override")) {
            throw new TranslatableCommandException("command.mutation.error.illegal", mutationString);
        }

        pollManager.startPoll(pollMutationFactory.create(sender, mutation));
        return null;
    }

    @Command(
            aliases = {"custom"},
            desc = "Start a poll with the supplied text",
            usage = "[text...]",
            min = 1,
            max = -1
    )
    @CommandPermissions("poll.custom")
    public void pollCustom(CommandContext args, CommandSender sender) throws CommandException {
        String text = args.getJoinedStrings(0);

        pollManager.startPoll(pollCustomFactory.create(text, sender));
    }
}
