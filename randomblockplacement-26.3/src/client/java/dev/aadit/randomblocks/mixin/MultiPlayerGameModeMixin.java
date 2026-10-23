package dev.aadit.randomblocks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.aadit.randomblocks.PlacementHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	// HEAD runs before useItemOn's own ensureHasSentCarriedItem() call, so a slot change
	// made here reaches the server before the placement packet does.
	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void randomblocks$pickBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (hand == InteractionHand.MAIN_HAND) PlacementHandler.beforeUseItemOn(player, hit);
	}
}
