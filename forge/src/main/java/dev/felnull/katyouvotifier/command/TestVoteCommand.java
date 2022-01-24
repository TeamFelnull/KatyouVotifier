package dev.felnull.katyouvotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import dev.felnull.katyouvotifier.handler.ServerHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class TestVoteCommand {
    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("testvote").requires((source) -> source.hasPermission(2))
                .then(Commands.argument("username", StringArgumentType.string()).then(Commands.argument("serviceName", StringArgumentType.string()).then(Commands.argument("address", StringArgumentType.string())
                        .then(Commands.argument("timestamp", LongArgumentType.longArg())
                                .executes((context -> testVote(context.getSource(), StringArgumentType.getString(context, "serviceName"), StringArgumentType.getString(context, "address"), StringArgumentType.getString(context, "username"), LongArgumentType.getLong(context, "timestamp")))))))));
    }

    public static int testVote(CommandSource source, String username, String serviceName, String address, long timestamp) {
        if (timestamp < 0)
            timestamp = System.currentTimeMillis();
        Vote vote = new Vote(username, serviceName, address, Long.toString(timestamp, 10));
        if (ServerHandler.getVotifierHandler() != null) {
            ServerHandler.getVotifierHandler().onVoteReceived(vote, VotifierSession.ProtocolVersion.TEST, "localhost.test");
            source.sendSuccess(new StringTextComponent("Test vote executed: " + vote).withStyle(TextFormatting.GREEN), false);
        }
        return 1;
    }
}
