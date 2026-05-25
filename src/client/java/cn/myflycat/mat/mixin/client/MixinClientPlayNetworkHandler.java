package cn.myflycat.mat.mixin.client;

import cn.myflycat.mat.client.recorder.Recorder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (!Recorder.getInstance().isRecording()) return;
        Recorder.getInstance().recordChat(message);
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private void onSendChatCommand(String command, CallbackInfo ci) {
        if (!Recorder.getInstance().isRecording()) return;
        // Record commands that are not MAT internal commands
        if (!command.startsWith("mat ")) {
            Recorder.getInstance().recordChat("/" + command);
        }
    }
}
