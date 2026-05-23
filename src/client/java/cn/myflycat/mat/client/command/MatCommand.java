package cn.myflycat.mat.client.command;

import cn.myflycat.mat.script.ScriptHandle;
import cn.myflycat.mat.script.ScriptManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;

import java.io.IOException;
import java.util.List;

public final class MatCommand {
    private static final int MAX_STACK_FRAMES = 6;

    private MatCommand() {}

    public static void register(ScriptManager manager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("mat")
                        .then(ClientCommandManager.literal("run")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests(availableScripts(manager))
                                        .executes(ctx -> runScript(ctx, manager))))
                        .then(ClientCommandManager.literal("stop")
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .suggests(runningScripts(manager))
                                        .executes(ctx -> stopScript(ctx, manager))))
                        .then(ClientCommandManager.literal("list")
                                .executes(ctx -> listScripts(ctx, manager)))
                        .then(ClientCommandManager.literal("reload")
                                .executes(ctx -> reloadCache(ctx, manager)))));
    }

    private static int runScript(CommandContext<FabricClientCommandSource> ctx, ScriptManager manager) {
        String name = StringArgumentType.getString(ctx, "name");
        try {
            ScriptHandle handle = manager.start(name);
            ctx.getSource().sendFeedback(Text.literal("Started script: " + name));
            handle.completion().whenComplete((v, t) -> notifyExit(ctx.getSource(), handle));
            return 1;
        } catch (IllegalStateException e) {
            ctx.getSource().sendError(Text.literal(e.getMessage()));
            return 0;
        }
    }

    private static int stopScript(CommandContext<FabricClientCommandSource> ctx, ScriptManager manager) {
        String name = StringArgumentType.getString(ctx, "name");
        if ("all".equalsIgnoreCase(name)) {
            manager.stopAll();
            ctx.getSource().sendFeedback(Text.literal("Cancellation requested for all running scripts"));
            return 1;
        }
        if (manager.stop(name)) {
            ctx.getSource().sendFeedback(Text.literal("Cancellation requested: " + name));
            return 1;
        }
        ctx.getSource().sendError(Text.literal("Not running: " + name));
        return 0;
    }

    private static int listScripts(CommandContext<FabricClientCommandSource> ctx, ScriptManager manager) {
        FabricClientCommandSource src = ctx.getSource();
        try {
            List<String> available = manager.availableScripts();
            src.sendFeedback(Text.literal("Available (" + available.size() + "): " +
                    (available.isEmpty() ? "<none>" : String.join(", ", available))));
        } catch (IOException e) {
            src.sendError(Text.literal("Failed to list scripts: " + e.getMessage()));
            return 0;
        }
        var running = manager.runningNames();
        src.sendFeedback(Text.literal("Running (" + running.size() + "): " +
                (running.isEmpty() ? "<none>" : String.join(", ", running))));
        return 1;
    }

    private static int reloadCache(CommandContext<FabricClientCommandSource> ctx, ScriptManager manager) {
        manager.reload();
        ctx.getSource().sendFeedback(Text.literal("Script source cache cleared"));
        return 1;
    }

    private static SuggestionProvider<FabricClientCommandSource> availableScripts(ScriptManager manager) {
        return (ctx, builder) -> {
            try {
                manager.availableScripts().forEach(builder::suggest);
            } catch (IOException ignored) {
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> runningScripts(ScriptManager manager) {
        return (ctx, builder) -> {
            builder.suggest("all");
            manager.runningNames().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static void notifyExit(FabricClientCommandSource src, ScriptHandle handle) {
        switch (handle.state()) {
            case DONE -> src.sendFeedback(Text.literal("Script finished: " + handle.name()));
            case CANCELLED -> src.sendFeedback(Text.literal("Script cancelled: " + handle.name()));
            case FAILED -> src.sendError(formatError(handle));
            default -> {}
        }
    }

    private static Text formatError(ScriptHandle handle) {
        Throwable t = handle.error();
        String head = "✗ script " + handle.name() + " failed: ";
        if (t == null) {
            return Text.literal(head + "(no error captured)");
        }
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName();
        }
        StringBuilder sb = new StringBuilder(head).append(msg);
        if (t instanceof PolyglotException pe) {
            int shown = 0;
            for (PolyglotException.StackFrame f : pe.getPolyglotStackTrace()) {
                if (shown >= MAX_STACK_FRAMES) break;
                if (f.isHostFrame()) continue;
                sb.append("\n  at ").append(formatFrame(f));
                shown++;
            }
        }
        return Text.literal(sb.toString());
    }

    private static String formatFrame(PolyglotException.StackFrame f) {
        String fn = f.getRootName();
        if (fn == null || fn.isEmpty()) fn = "<anonymous>";
        SourceSection loc = f.getSourceLocation();
        if (loc == null || loc.getSource() == null) return fn;
        int line = loc.getStartLine() - 1;
        String lineStr = line < 1 ? "?" : Integer.toString(line);
        return fn + " (" + loc.getSource().getName() + ":" + lineStr + ")";
    }
}
