package cn.myflycat.mat.script;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class ModuleRegistry {
    private static final String WRAPPER_HEAD = "(function (module, exports, require) {\n";
    private static final String WRAPPER_TAIL = "\n});";

    private final Context ctx;
    private final ScriptSourceLoader loader;
    private final Map<String, Value> cache = new HashMap<>();
    private final LinkedHashSet<String> loadStack = new LinkedHashSet<>();

    public ModuleRegistry(Context ctx, ScriptSourceLoader loader) {
        this.ctx = ctx;
        this.loader = loader;
        ProxyExecutable req = args -> {
            if (args.length == 0 || args[0] == null || args[0].isNull()) {
                throw new RuntimeException("require: missing module name");
            }
            return requireModule(args[0].asString());
        };
        ctx.getBindings(ScriptEngine.LANGUAGE).putMember("require", req);
    }

    public Value runEntry(String name) throws IOException {
        return load(name);
    }

    private Value requireModule(String name) {
        Value cached = cache.get(name);
        if (cached != null) return cached;
        try {
            return load(name);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load module " + name + ": " + e.getMessage(), e);
        }
    }

    private Value load(String name) throws IOException {
        if (loadStack.contains(name)) {
            String chain = String.join(" -> ", loadStack) + " -> " + name;
            throw new IOException("Cyclic require: " + chain);
        }
        loadStack.add(name);
        try {
            ScriptSource src = loader.load(name);
            CharSequence raw = src.source().getCharacters();
            Source wrapped = Source.newBuilder(ScriptEngine.LANGUAGE,
                    WRAPPER_HEAD + raw + WRAPPER_TAIL, name).buildLiteral();
            Value moduleFn = ctx.eval(wrapped);
            Value moduleObj = ctx.eval(ScriptEngine.LANGUAGE, "({exports:{}})");
            Value requireFn = ctx.getBindings(ScriptEngine.LANGUAGE).getMember("require");
            moduleFn.execute(moduleObj, moduleObj.getMember("exports"), requireFn);
            Value exports = moduleObj.getMember("exports");
            cache.put(name, exports);
            return exports;
        } finally {
            loadStack.remove(name);
        }
    }
}
