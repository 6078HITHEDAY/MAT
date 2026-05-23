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

import java.io.IOException;
import java.util.List;

public final class MatCommand {
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
            case FAILED -> src.sendError(Text.literal("Script " + handle.name() + " failed: " +
                    (handle.error() == null ? "(unknown)" : handle.error().getMessage())));
            default -> {}
        }
    }
}
