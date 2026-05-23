package cn.myflycat.mat.client.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.graalvm.polyglot.HostAccess;

public final class ChatBindings {
    @HostAccess.Export
    public void log(String message) {
        String text = String.valueOf(message);
        ClientThread.runSyncVoid(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().addMessage(Text.literal(text));
        });
    }

    @HostAccess.Export
    public void send(String message) {
        String text = String.valueOf(message);
        ClientThread.runSyncVoid(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;
            if (text.startsWith("/")) {
                player.networkHandler.sendChatCommand(text.substring(1));
            } else {
                player.networkHandler.sendChatMessage(text);
            }
        });
    }
}
