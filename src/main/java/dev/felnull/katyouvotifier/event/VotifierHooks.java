package dev.felnull.katyouvotifier.event;

import com.vexsoftware.votifier.model.Vote;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class VotifierHooks {
    public static void onVote(Vote vote) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null)
            server.addScheduledTask(() -> {
                MinecraftForge.EVENT_BUS.post(new VotifierEvent(vote));
            });
    }
}
