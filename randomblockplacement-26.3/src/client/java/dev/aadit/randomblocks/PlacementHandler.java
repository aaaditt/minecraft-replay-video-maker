package dev.aadit.randomblocks;

import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chooses the next block right before a placement and puts it in the player's hand.
 * Everything here goes through normal client actions (slot select, inventory swap click),
 * so it also works in survival on multiplayer servers.
 */
public final class PlacementHandler {
	private static final Random RNG = new Random();

	private PlacementHandler() {
	}

	/** Called from the useItemOn HEAD mixin, before vanilla syncs the selected slot. */
	public static void beforeUseItemOn(LocalPlayer player, BlockHitResult hit) {
		Palette palette = Palette.get();
		if (!palette.active) return;

		List<Palette.Entry> entries = palette.enabledEntries();
		if (entries.isEmpty()) return;
		// Only take over while the player is holding one of the palette blocks (or nothing),
		// so using a door, chest or tool still behaves normally.
		ItemStack held = player.getInventory().getSelectedItem();
		if (!held.isEmpty() && indexOf(entries, held.getItem()) < 0) return;

		Inventory inv = player.getInventory();
		int[] weights = new int[entries.size()];
		boolean[] available = new boolean[entries.size()];
		boolean any = false;
		for (int i = 0; i < entries.size(); i++) {
			weights[i] = entries.get(i).weight;
			available[i] = findSlot(inv, entries.get(i).resolve()) >= 0;
			any |= available[i];
		}
		if (!any) {
			palette.active = false;
			player.sendOverlayMessage(Component.translatable("message.randomblocks.out_of_blocks"));
			return;
		}

		int pick;
		if (palette.mode == Palette.Mode.GRADIENT) {
			BlockPos target = targetPos(player, hit);
			if (palette.anchor == null) {
				palette.anchor = target;
				palette.anchorDirection = gradientDirection(player, palette.axis);
			}
			Direction dir = palette.anchorDirection;
			double t = BlockPicker.gradientT(
				target.getX() - palette.anchor.getX(),
				target.getY() - palette.anchor.getY(),
				target.getZ() - palette.anchor.getZ(),
				dir.getStepX(), dir.getStepY(), dir.getStepZ(),
				palette.gradientLength);
			pick = BlockPicker.pickGradient(weights, available, t, palette.blend, RNG);
		} else {
			pick = BlockPicker.pickWeighted(weights, available, RNG);
		}
		if (pick >= 0) equip(player, entries.get(pick).resolve());
	}

	/** Puts a palette block in hand right away, so the first placement after pressing Place counts too. */
	public static void equipAnyPaletteBlock(LocalPlayer player) {
		for (Palette.Entry e : Palette.get().enabledEntries()) {
			if (equip(player, e.resolve())) return;
		}
	}

	private static boolean equip(LocalPlayer player, Item item) {
		Inventory inv = player.getInventory();
		int slot = findSlot(inv, item);
		if (slot < 0) return false;
		if (Inventory.isHotbarSlot(slot)) {
			inv.setSelectedSlot(slot);
			return true;
		}
		// Main inventory slots 9..35 have the same index in the player's inventory menu.
		// SWAP with the selected hotbar slot as the button is the same as pressing that
		// number key while hovering the slot.
		Minecraft mc = Minecraft.getInstance();
		mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, inv.getSelectedSlot(),
			ContainerInput.SWAP, player);
		return true;
	}

	/** Hotbar first, then main inventory. Returns -1 if the player has none. */
	private static int findSlot(Inventory inv, Item item) {
		if (item == null) return -1;
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.isEmpty() && stack.getItem() == item) return i;
		}
		return -1;
	}

	private static int indexOf(List<Palette.Entry> entries, Item item) {
		String id = BuiltInRegistries.ITEM.getKey(item).toString();
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).item.equals(id)) return i;
		}
		return -1;
	}

	private static BlockPos targetPos(LocalPlayer player, BlockHitResult hit) {
		BlockPos clicked = hit.getBlockPos();
		return player.level().getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
	}

	private static Direction gradientDirection(LocalPlayer player, Palette.Axis axis) {
		return switch (axis) {
			case X -> Direction.EAST;
			case Y -> Direction.UP;
			case Z -> Direction.SOUTH;
			case AUTO -> Direction.orderedByNearest(player)[0];
		};
	}
}
