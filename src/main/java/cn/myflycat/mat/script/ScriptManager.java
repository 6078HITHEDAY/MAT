package cn.myflycat.mat.script;

import cn.myflycat.mat.Mat;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ScriptManager implements AutoCloseable {
    private final ScriptEngine engine;
    private final ScriptSourceLoader loader;
    private final Function<ScriptHandle, Object> apiFactory;
    private final Map<String, ScriptHandle> running = new ConcurrentHashMap<>();

    public ScriptManager(ScriptEngine engine, ScriptSourceLoader loader) {
        this(engine, loader, null);
    }

    public ScriptManager(ScriptEngine engine, ScriptSourceLoader loader,
                         Function<ScriptHandle, Object> apiFactory) {
        this.engine = engine;
        this.loader = loader;
        this.apiFactory = apiFactory;
    }

    public ScriptSourceLoader loader() {
        return loader;
    }

    public Set<String> runningNames() {
        return Set.copyOf(running.keySet());
    }

    public List<String> availableScripts() throws IOException {
        return loader.listScripts();
    }

    public ScriptHandle start(String name) {
        ScriptHandle handle = new ScriptHandle(name);
        ScriptHandle prev = running.putIfAbsent(name, handle);
        if (prev != null) {
            throw new IllegalStateException("Script already running: " + name);
        }
        Thread t = new Thread(() -> runScript(handle), "MAT-script-" + name);
        t.setDaemon(true);
        t.start();
        return handle;
    }

    public boolean stop(String name) {
        ScriptHandle handle = running.get(name);
        if (handle == null) {
            return false;
        }
        handle.requestCancel();
        return true;
    }

    public void stopAll() {
        running.values().forEach(ScriptHandle::requestCancel);
    }

    public void reload() {
        loader.invalidate();
    }

    private void runScript(ScriptHandle handle) {
        if (!handle.markRunning()) {
            running.remove(handle.name());
            return;
        }
        try (Context ctx = engine.newContext()) {
            if (apiFactory != null) {
                Object api = apiFactory.apply(handle);
                if (api != null) {
                    ctx.getBindings("js").putMember("mat", api);
                }
            }
            ModuleRegistry modules = new ModuleRegistry(ctx, loader);
            modules.runEntry(handle.name());
            if (handle.isCancelRequested()) {
                handle.markCancelled();
            } else {
                handle.markDone();
            }
        } catch (PolyglotException e) {
            Mat.LOGGER.error("Script {} threw: {}", handle.name(), e.getMessage());
            handle.markFailed(e);
        } catch (Throwable t) {
            Mat.LOGGER.error("Script {} failed to load/execute", handle.name(), t);
            handle.markFailed(t);
        } finally {
            running.remove(handle.name());
        }
    }

    @Override
    public void close() {
        stopAll();
        engine.close();
    }
}
