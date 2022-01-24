package dev.felnull.katyouvotifier.event;

import com.vexsoftware.votifier.model.Vote;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;

public class VotifierHooks {
    public static void onVote(Vote vote) {
        MinecraftServer server = LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
        if (server != null)
            server.submit(() -> MinecraftForge.EVENT_BUS.post(new VotifierEvent(vote)));
    }
}
