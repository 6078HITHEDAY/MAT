package cn.myflycat.mat.script;

import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ScriptSourceLoader {
    private static final String SCRIPT_EXT = ".js";
    private static final ThreadLocal<Deque<String>> RESOLUTION_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final Path root;
    private final Map<String, ScriptSource> cache = new ConcurrentHashMap<>();

    public ScriptSourceLoader(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public ScriptSource load(String name) throws IOException {
        Path target = resolveInsideRoot(name);
        long mtime = Files.getLastModifiedTime(target).toMillis();

        ScriptSource cached = cache.get(name);
        if (cached != null && cached.mtimeMillis() == mtime) {
            return cached;
        }

        Deque<String> stack = RESOLUTION_STACK.get();
        if (stack.contains(name)) {
            throw new IOException("Cyclic script import: " + String.join(" -> ", stack) + " -> " + name);
        }
        stack.push(name);
        try {
            Source source = Source.newBuilder(ScriptEngine.LANGUAGE, target.toFile())
                    .name(name)
                    .build();
            ScriptSource scriptSource = new ScriptSource(name, target, mtime, source);
            cache.put(name, scriptSource);
            return scriptSource;
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                RESOLUTION_STACK.remove();
            }
        }
    }

    public void invalidate() {
        cache.clear();
    }

    public List<String> listScripts() throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.list(root)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(SCRIPT_EXT))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - SCRIPT_EXT.length()))
                    .sorted()
                    .toList();
        }
    }

    private Path resolveInsideRoot(String name) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IOException("Script name must not be blank");
        }
        Path resolved = root.resolve(name + SCRIPT_EXT).normalize();
        if (!resolved.startsWith(root)) {
            throw new IOException("Script name escapes script root: " + name);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IOException("Script not found: " + resolved);
        }
        return resolved;
    }
}
