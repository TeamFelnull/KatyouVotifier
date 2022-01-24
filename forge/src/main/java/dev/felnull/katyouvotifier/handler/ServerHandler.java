package dev.felnull.katyouvotifier.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import dev.felnull.katyouvotifier.ForgeScheduler;
import dev.felnull.katyouvotifier.KatyouVotifierForge;
import dev.felnull.katyouvotifier.command.KVReloadCommand;
import dev.felnull.katyouvotifier.command.TestVoteCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ForgeScheduler.ForgeTask> tasks = new ArrayList<>();
    private static final List<ForgeScheduler.ForgeTask> removed = new ArrayList<>();
    private static VotifierServerBootstrap bootstrap;
    private static ForgeScheduler scheduler;
    private static NuVotifierHandler votifierHandler;

    @SubscribeEvent
    public static void onServetTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        for (ForgeScheduler.ForgeTask task : tasks) {
            if (task.isCancel()) {
                removed.add(task);
            } else if (System.currentTimeMillis() - task.getStartTime() >= task.getDelay()) {
                ForgeScheduler.runTask(LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER), task.getRunnable(), task.isAsync());
                removed.add(task);
            }
        }
        tasks.removeAll(removed);
        removed.clear();
    }

    @SubscribeEvent
    public static void onServetStart(FMLServerStartingEvent e) {
        tasks.clear();
        removed.clear();
        KatyouVotifierForge.LOGGER.info("Votifier start.");
        loadVotifier(e.getServer());
    }

    @SubscribeEvent
    public static void onServetStop(FMLServerStoppingEvent e) {
        tasks.clear();
        removed.clear();
        halt();
        KatyouVotifierForge.LOGGER.info("Votifier stop.");
        scheduler = null;
        votifierHandler = null;
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent e) {
        KVReloadCommand.register(e.getDispatcher());
        TestVoteCommand.register(e.getDispatcher());
    }

    public static void loadVotifier(MinecraftServer server) {
        scheduler = new ForgeScheduler(server);
        File config = new File(FMLPaths.CONFIGDIR.get().resolve(KatyouVotifierForge.MODID).toFile(), "config.json");
        JsonObject jo = getOrGenerateConfig(config);
        String host = jo.get("host").getAsString();
        int port = jo.get("port").getAsInt();
        boolean disablev1 = jo.get("disable-v1-protocol").getAsBoolean();

        Map<String, Key> tokens = new HashMap<>();
        JsonObject tjo = jo.getAsJsonObject("tokens");
        for (Map.Entry<String, JsonElement> entry : tjo.entrySet()) {
            tokens.put(entry.getKey(), KeyCreator.createKeyFrom(entry.getValue().getAsString()));
        }

        boolean debug = jo.get("debug").getAsBoolean();

        File rsaDirectory = new File(FMLPaths.CONFIGDIR.get().resolve(KatyouVotifierForge.MODID).toFile(), "rsa");

        KeyPair keyPair;
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            KatyouVotifierForge.LOGGER.error("Error reading configuration file or RSA tokens", ex);
            return;
        }

        votifierHandler = new NuVotifierHandler(scheduler, KatyouVotifierForge.LOGGER, tokens, keyPair, debug);

        bootstrap = new VotifierServerBootstrap(host, port, votifierHandler, disablev1);
        bootstrap.start(error -> {
        });
    }

    public static JsonObject getOrGenerateConfig(File configFile) {
        if (configFile.exists()) {
            try {
                String jp = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
                return GSON.fromJson(jp, JsonObject.class);
            } catch (IOException ignored) {
            }
        }
        KatyouVotifierForge.LOGGER.info("Configuring Votifier for the first time...");

        JsonObject jo = new JsonObject();
        jo.addProperty("host", "0.0.0.0");
        jo.addProperty("port", 8192);
        jo.addProperty("disable-v1-protocol", false);

        JsonObject tokenJo = new JsonObject();
        String token = TokenUtil.newToken();
        tokenJo.addProperty("default", token);
        jo.add("tokens", tokenJo);

        jo.addProperty("debug", false);

        KatyouVotifierForge.LOGGER.info("------------------------------------------------------------------------------");
        KatyouVotifierForge.LOGGER.info("Assigning NuVotifier to listen on port 8192.");
        KatyouVotifierForge.LOGGER.info("------------------------------------------------------------------------------");
        KatyouVotifierForge.LOGGER.info("Your default NuVotifier token is " + token + ".");
        KatyouVotifierForge.LOGGER.info("You will need to provide this token when you submit your server to a voting");
        KatyouVotifierForge.LOGGER.info("list.");
        KatyouVotifierForge.LOGGER.info("------------------------------------------------------------------------------");

        if (!configFile.getParentFile().exists()) {
            if (!configFile.getParentFile().mkdirs())
                KatyouVotifierForge.LOGGER.error("Failed to create config directory!");
        } else {
            try {
                Files.write(configFile.toPath(), GSON.toJson(jo).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
                KatyouVotifierForge.LOGGER.error("Failed to write config file!", e);
            }
        }
        return jo;
    }

    public static void halt() {
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }
    }

    public static boolean reload(MinecraftServer server) {
        try {
            halt();
        } catch (Exception ex) {
            KatyouVotifierForge.LOGGER.error("On halt, an exception was thrown. This may be fine!", ex);
        }
        try {
            loadVotifier(server);
            KatyouVotifierForge.LOGGER.info("Reload was successful.");
            return true;
        } catch (Exception ex) {
            try {
                halt();
                KatyouVotifierForge.LOGGER.error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex2) {
                KatyouVotifierForge.LOGGER.error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex2);
            }
            return false;
        }
    }

    public static NuVotifierHandler getVotifierHandler() {
        return votifierHandler;
    }

    public static void addTask(ForgeScheduler.ForgeTask task) {
        tasks.add(task);
    }
}
