package cn.myflycat.mat.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;

public final class ScriptEngine implements AutoCloseable {
    static final String LANGUAGE = "js";

    private final Engine engine;
    private final HostAccess hostAccess;

    public ScriptEngine() {
        this(HostAccess.EXPLICIT);
    }

    public ScriptEngine(HostAccess hostAccess) {
        this.engine = Engine.newBuilder(LANGUAGE)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        this.hostAccess = hostAccess;
    }

    public Engine engine() {
        return engine;
    }

    public Context newContext() {
        return Context.newBuilder(LANGUAGE)
                .engine(engine)
                .allowHostAccess(hostAccess)
                .allowHostClassLookup(name -> false)
                .allowNativeAccess(false)
                .allowCreateThread(false)
                .allowCreateProcess(false)
                .allowEnvironmentAccess(EnvironmentAccess.NONE)
                .build();
    }

    @Override
    public void close() {
        engine.close();
    }
}
