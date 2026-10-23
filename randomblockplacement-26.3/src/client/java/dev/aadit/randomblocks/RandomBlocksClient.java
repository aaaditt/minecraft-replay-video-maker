package dev.aadit.randomblocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;

import dev.aadit.randomblocks.screen.PaletteScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class RandomBlocksClient implements ClientModInitializer {
	public static final String MOD_ID = "randomblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(id("keys"));
	private static KeyMapping openKey;
	private static KeyMapping toggleKey;
	private static KeyMapping resetAnchorKey;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		openKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.randomblocks.open", InputConstants.KEY_R, CATEGORY));
		toggleKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.randomblocks.toggle", InputConstants.KEY_G, CATEGORY));
		resetAnchorKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.randomblocks.reset_anchor", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(RandomBlocksClient::onTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, id("palette"), PaletteHud::extractRenderState);
	}

	private static void onTick(Minecraft mc) {
		if (mc.player == null) return;
		while (openKey.consumeClick()) {
			mc.gui.setScreen(new PaletteScreen());
		}
		while (toggleKey.consumeClick()) {
			Palette palette = Palette.get();
			if (palette.active) {
				stop();
			} else if (palette.enabledEntries().isEmpty()) {
				mc.gui.setScreen(new PaletteScreen());
			} else {
				start();
			}
		}
		while (resetAnchorKey.consumeClick()) {
			Palette.get().resetAnchor();
			mc.player.sendOverlayMessage(Component.translatable("message.randomblocks.anchor_reset"));
		}
	}

	public static void start() {
		Minecraft mc = Minecraft.getInstance();
		Palette palette = Palette.get();
		if (mc.player == null) return;
		if (palette.enabledEntries().isEmpty()) {
			mc.player.sendOverlayMessage(Component.translatable("screen.randomblocks.none_selected"));
			return;
		}
		palette.active = true;
		palette.resetAnchor();
		PlacementHandler.equipAnyPaletteBlock(mc.player);
		String mode = palette.mode == Palette.Mode.GRADIENT ? "gradient" : "random";
		mc.player.sendOverlayMessage(Component.translatable("message.randomblocks.started." + mode));
	}

	public static void stop() {
		Minecraft mc = Minecraft.getInstance();
		Palette.get().active = false;
		if (mc.player != null) mc.player.sendOverlayMessage(Component.translatable("message.randomblocks.stopped"));
	}
}
