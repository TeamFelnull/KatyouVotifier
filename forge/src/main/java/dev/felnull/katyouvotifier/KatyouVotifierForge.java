package dev.felnull.katyouvotifier;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import dev.felnull.katyouvotifier.handler.ServerHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;

@Mod(KatyouVotifierForge.MODID)
public class KatyouVotifierForge {
    public static final String MODID = "katyouvotifier";
    public static LoggingAdapter LOGGER;

    public KatyouVotifierForge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(ServerHandler.class);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER = new Log4jLoggerAdapter(LogManager.getLogger());
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
