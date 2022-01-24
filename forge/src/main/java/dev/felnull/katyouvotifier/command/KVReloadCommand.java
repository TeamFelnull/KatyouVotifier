package dev.felnull.katyouvotifier.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.felnull.katyouvotifier.KatyouVotifierForge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public class KVReloadCommand {
    public static void register(CommandDispatcher<CommandSource> d) {
        d.register(Commands.literal("kvrload").requires((source) -> source.hasPermission(2)).executes((context -> reload(context.getSource()))));
    }

    private static int reload(CommandSource src) {
        if (KatyouVotifierForge.reloadVotifier(src.getServer())) {
            src.sendSuccess(new StringTextComponent("KatyouVotifier has been reloaded!").withStyle(TextFormatting.DARK_GREEN), false);
        } else {
            src.sendFailure(new StringTextComponent("Looks like there was a problem reloading KatyouVotifier, check the console!"));
        }
        return 1;
    }
}
