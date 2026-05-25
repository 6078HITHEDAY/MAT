package cn.myflycat.mat.client.recorder;

import cn.myflycat.mat.Mat;
import cn.myflycat.mat.recorder.ActionRecord;
import cn.myflycat.mat.recorder.ActionType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Recorder {
    private static final Recorder INSTANCE = new Recorder();
    private static final int MOVE_TICK_INTERVAL = 10;
    private static final double MOVE_THRESHOLD = 0.5;

    private volatile boolean recording = false;
    private long startTime;
    private final List<ActionRecord> records = new CopyOnWriteArrayList<>();
    private Vec3d lastPos;
    private int tickCounter;

    private Recorder() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.onTick());
    }

    public static Recorder getInstance() { return INSTANCE; }

    public void start() {
        records.clear();
        startTime = System.currentTimeMillis();
        lastPos = null;
        tickCounter = 0;
        recording = true;
        Mat.LOGGER.info("Recording started");
    }

    public List<ActionRecord> stop() {
        recording = false;
        List<ActionRecord> snapshot = List.copyOf(records);
        Mat.LOGGER.info("Recording stopped: {} actions captured", snapshot.size());
        return snapshot;
    }

    public boolean isRecording() { return recording; }

    public List<ActionRecord> records() { return List.copyOf(records); }

    public boolean save(Path outputPath) {
        try {
            List<ActionRecord> snapshot = List.copyOf(records);
            if (snapshot.isEmpty()) {
                Mat.LOGGER.warn("No records to save");
                return false;
            }
            String code = CodeGenerator.generate(snapshot, startTime);
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, code);
            Mat.LOGGER.info("Saved {} actions to {}", snapshot.size(), outputPath);
            return true;
        } catch (IOException e) {
            Mat.LOGGER.error("Failed to save recording: {}", e.getMessage());
            return false;
        }
    }

    // === Event receivers (called from mixins / tick) ===

    public void recordBreakBlock(BlockPos pos, BlockState state) {
        if (!recording) return;
        Map<String, Object> params = new HashMap<>();
        params.put("x", pos.getX());
        params.put("y", pos.getY());
        params.put("z", pos.getZ());
        if (state != null) params.put("block", state.getBlock().getName().getString());
        addRecord(ActionType.BREAK_BLOCK, params);
    }

    public void recordPlaceBlock(BlockPos pos, BlockState state) {
        if (!recording) return;
        Map<String, Object> params = new HashMap<>();
        params.put("x", pos.getX());
        params.put("y", pos.getY());
        params.put("z", pos.getZ());
        if (state != null) params.put("block", state.getBlock().getName().getString());
        addRecord(ActionType.PLACE_BLOCK, params);
    }

    public void recordUseItem(String hand, String itemId) {
        if (!recording) return;
        Map<String, Object> params = new HashMap<>();
        params.put("hand", hand);
        if (itemId != null) params.put("item", itemId);
        addRecord(ActionType.USE_ITEM, params);
    }

    public void recordAttackEntity(Entity target) {
        if (!recording || target == null) return;
        Map<String, Object> params = new HashMap<>();
        params.put("entity", target.getName().getString());
        params.put("entityType", target.getType().getName().getString());
        addRecord(ActionType.ATTACK_ENTITY, params);
    }

    public void recordInteractEntity(Entity target, String hand) {
        if (!recording || target == null) return;
        Map<String, Object> params = new HashMap<>();
        params.put("entity", target.getName().getString());
        params.put("entityType", target.getType().getName().getString());
        params.put("hand", hand);
        addRecord(ActionType.INTERACT_ENTITY, params);
    }

    public void recordChat(String message) {
        if (!recording || message == null || message.isEmpty()) return;
        Map<String, Object> params = new HashMap<>();
        params.put("message", message);
        addRecord(ActionType.CHAT, params);
    }

    // === Internal ===

    private void addRecord(ActionType type, Map<String, Object> params) {
        long timestamp = System.currentTimeMillis() - startTime;
        records.add(new ActionRecord(type, timestamp, params));
    }

    private void onTick() {
        if (!recording) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        tickCounter++;
        if (tickCounter % MOVE_TICK_INTERVAL != 0) return;

        Vec3d pos = player.getPos();
        if (lastPos == null || pos.squaredDistanceTo(lastPos) > MOVE_THRESHOLD * MOVE_THRESHOLD) {
            Map<String, Object> params = new HashMap<>();
            params.put("x", pos.x);
            params.put("y", pos.y);
            params.put("z", pos.z);
            params.put("yaw", (double) player.getYaw());
            params.put("pitch", (double) player.getPitch());
            addRecord(ActionType.MOVE, params);
            lastPos = pos;
        }
    }
}
