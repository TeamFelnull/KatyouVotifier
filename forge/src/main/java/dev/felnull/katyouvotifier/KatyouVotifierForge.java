package dev.felnull.katyouvotifier;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import dev.felnull.katyouvotifier.handler.ServerHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;

@Mod(KatyouVotifierForge.MODID)
public class KatyouVotifierForge {
    public static final String MODID = "katyouvotifier";
    public static final LoggingAdapter LOGGER = new Log4jLoggerAdapter(LogManager.getLogger());

    public KatyouVotifierForge() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(ServerHandler.class);
    }

    public static void startVotifier(MinecraftServer server) {
        ServerHandler.loadVotifier(server);
    }

    public static void stopVotifier() {
        ServerHandler.halt();
    }

    public static boolean reloadVotifier(MinecraftServer server) {
        return ServerHandler.reload(server);
    }
}
