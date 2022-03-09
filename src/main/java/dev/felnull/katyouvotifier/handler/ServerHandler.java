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
import dev.felnull.katyouvotifier.KatyouVotifier;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<ForgeScheduler.ForgeTask> tasks = new ArrayList<>();
    public static final List<ForgeScheduler.ForgeTask> removed = new ArrayList<>();
    public static VotifierServerBootstrap bootstrap;
    public static ForgeScheduler scheduler;
    public static NuVotifierHandler votifierHandler;

    @SubscribeEvent
    public static void onServetTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        for (ForgeScheduler.ForgeTask task : tasks) {
            if (task.isCancel()) {
                removed.add(task);
            } else if (System.currentTimeMillis() - task.getStartTime() >= task.getDelay()) {
                ForgeScheduler.runTask(FMLCommonHandler.instance().getMinecraftServerInstance(), task.getRunnable(), task.isAsync());
                removed.add(task);
            }
        }
        tasks.removeAll(removed);
        removed.clear();
    }


/*
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent e) {
        KVReloadCommand.register(e.getDispatcher());
        TestVoteCommand.register(e.getDispatcher());
    }*/

    public static void loadVotifier(MinecraftServer server) {
        scheduler = new ForgeScheduler(server);
        // File config = new File(FMLPaths.CONFIGDIR.get().resolve(KatyouVotifier.MODID).toFile(), "config.json");
        File config = new File(Paths.get("./").resolve(KatyouVotifier.MODID).toFile(), "config.json");

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

        //   File rsaDirectory = new File(FMLPaths.CONFIGDIR.get().resolve(KatyouVotifier.MODID).toFile(), "rsa");
        File rsaDirectory = new File(Paths.get("./").resolve(KatyouVotifier.MODID).toFile(), "rsa");

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
            KatyouVotifier.LOGGER.error("Error reading configuration file or RSA tokens", ex);
            return;
        }

        votifierHandler = new NuVotifierHandler(scheduler, KatyouVotifier.LOGGER, tokens, keyPair, debug);

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
        KatyouVotifier.LOGGER.info("Configuring Votifier for the first time...");

        JsonObject jo = new JsonObject();
        jo.addProperty("host", "0.0.0.0");
        jo.addProperty("port", 8192);
        jo.addProperty("disable-v1-protocol", false);

        JsonObject tokenJo = new JsonObject();
        String token = TokenUtil.newToken();
        tokenJo.addProperty("default", token);
        jo.add("tokens", tokenJo);

        jo.addProperty("debug", false);

        KatyouVotifier.LOGGER.info("------------------------------------------------------------------------------");
        KatyouVotifier.LOGGER.info("Assigning NuVotifier to listen on port 8192.");
        KatyouVotifier.LOGGER.info("------------------------------------------------------------------------------");
        KatyouVotifier.LOGGER.info("Your default NuVotifier token is " + token + ".");
        KatyouVotifier.LOGGER.info("You will need to provide this token when you submit your server to a voting");
        KatyouVotifier.LOGGER.info("list.");
        KatyouVotifier.LOGGER.info("------------------------------------------------------------------------------");

        if (!configFile.getParentFile().exists()) {
            if (!configFile.getParentFile().mkdirs()) {
                KatyouVotifier.LOGGER.error("Failed to create config directory!");
                return jo;
            }
        }

        try {
            Files.write(configFile.toPath(), GSON.toJson(jo).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            KatyouVotifier.LOGGER.error("Failed to write config file!", e);
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
            KatyouVotifier.LOGGER.error("On halt, an exception was thrown. This may be fine!", ex);
        }
        try {
            loadVotifier(server);
            KatyouVotifier.LOGGER.info("Reload was successful.");
            return true;
        } catch (Exception ex) {
            try {
                halt();
                KatyouVotifier.LOGGER.error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex2) {
                KatyouVotifier.LOGGER.error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex2);
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