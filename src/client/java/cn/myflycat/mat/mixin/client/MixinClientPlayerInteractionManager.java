package cn.myflycat.mat.mixin.client;

import cn.myflycat.mat.client.recorder.Recorder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world != null) {
            Recorder.getInstance().recordBreakBlock(pos, mc.world.getBlockState(pos));
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hit,
                                 CallbackInfoReturnable<ActionResult> cir) {
        // Only record as PLACE_BLOCK when holding a block item; ignore container opens etc.
        if (player.getStackInHand(hand).getItem() instanceof BlockItem) {
            net.minecraft.block.BlockState state = player.getWorld().getBlockState(hit.getBlockPos());
            Recorder.getInstance().recordPlaceBlock(hit.getBlockPos(), state);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"))
    private void onInteractItem(ClientPlayerEntity player, Hand hand,
                                CallbackInfoReturnable<ActionResult> cir) {
        var item = player.getStackInHand(hand).getItem();
        String itemId = Registries.ITEM.getId(item).toString();
        Recorder.getInstance().recordUseItem(hand.name(), itemId);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        Recorder.getInstance().recordAttackEntity(target);
    }

    @Inject(method = "interactEntity", at = @At("HEAD"))
    private void onInteractEntity(PlayerEntity player, Entity target, Hand hand,
                                  CallbackInfoReturnable<ActionResult> cir) {
        Recorder.getInstance().recordInteractEntity(target, hand.name());
    }
}
