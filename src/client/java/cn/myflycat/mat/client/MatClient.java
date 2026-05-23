package cn.myflycat.mat.client;

import cn.myflycat.mat.Mat;
import cn.myflycat.mat.client.command.MatCommand;
import cn.myflycat.mat.script.ScriptEngine;
import cn.myflycat.mat.script.ScriptManager;
import cn.myflycat.mat.script.ScriptSourceLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MatClient implements ClientModInitializer {
    private static ScriptManager scriptManager;

    public static ScriptManager scriptManager() {
        return scriptManager;
    }

    @Override
    public void onInitializeClient() {
        Path scriptsRoot = FabricLoader.getInstance().getConfigDir().resolve(Mat.MOD_ID).resolve("scripts");
        try {
            Files.createDirectories(scriptsRoot);
        } catch (IOException e) {
            Mat.LOGGER.error("Failed to create scripts directory {}: {}", scriptsRoot, e.getMessage());
        }

        ScriptEngine engine = new ScriptEngine();
        ScriptSourceLoader loader = new ScriptSourceLoader(scriptsRoot);
        scriptManager = new ScriptManager(engine, loader);
        Mat.LOGGER.info("Script root: {}", scriptsRoot);

        MatCommand.register(scriptManager);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (scriptManager != null) {
                scriptManager.close();
            }
        });
    }
}
