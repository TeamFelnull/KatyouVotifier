package dev.felnull.katyouvotifier;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import dev.felnull.katyouvotifier.handler.ServerHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = KatyouVotifier.MODID, name = KatyouVotifier.NAME, version = KatyouVotifier.VERSION, acceptableRemoteVersions = "*")
public class KatyouVotifier {
    public static final String MODID = "katyouvotifier";
    public static final String NAME = "Katyou Votifier";
    public static final String VERSION = "1.0";
    public static LoggingAdapter LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = new Log4jLoggerAdapter(event.getModLog());
        MinecraftForge.EVENT_BUS.register(ServerHandler.class);
    }

    @Mod.EventHandler
    public void onServetStart(FMLServerStartedEvent e) {
        ServerHandler.tasks.clear();
        ServerHandler.removed.clear();
        KatyouVotifier.LOGGER.info("Votifier start.");
        ServerHandler.loadVotifier(FMLCommonHandler.instance().getMinecraftServerInstance());
    }

    @Mod.EventHandler
    public void onServetStop(FMLServerStoppingEvent e) {
        ServerHandler.tasks.clear();
        ServerHandler.removed.clear();
        ServerHandler.halt();
        KatyouVotifier.LOGGER.info("Votifier stop.");
        ServerHandler.scheduler = null;
        ServerHandler.votifierHandler = null;
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
