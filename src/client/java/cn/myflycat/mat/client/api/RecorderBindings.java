package cn.myflycat.mat.client.api;

import cn.myflycat.mat.client.recorder.Recorder;
import net.fabricmc.loader.api.FabricLoader;
import org.graalvm.polyglot.HostAccess;

import java.nio.file.Path;

public final class RecorderBindings {
    private final Path scriptsRoot;

    public RecorderBindings() {
        this.scriptsRoot = FabricLoader.getInstance().getConfigDir()
                .resolve("mat").resolve("scripts");
    }

    @HostAccess.Export
    public boolean start() {
        Recorder.getInstance().start();
        return true;
    }

    @HostAccess.Export
    public boolean stop() {
        if (!Recorder.getInstance().isRecording()) return false;
        Recorder.getInstance().stop();
        return true;
    }

    @HostAccess.Export
    public boolean isRecording() {
        return Recorder.getInstance().isRecording();
    }

    @HostAccess.Export
    public boolean save(String name) {
        if (Recorder.getInstance().isRecording()) return false;
        String filename = name.endsWith(".js") ? name : name + ".js";
        Path out = scriptsRoot.resolve(filename);
        return Recorder.getInstance().save(out);
    }
}
