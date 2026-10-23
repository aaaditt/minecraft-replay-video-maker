package dev.aadit.randomblocks.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.aadit.randomblocks.Palette;
import dev.aadit.randomblocks.RandomBlocksClient;
import dev.aadit.randomblocks.screen.PaletteScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plays the mod in survival: singleplayer first, then on a dedicated server that does not
 * have the mod installed (like a vanilla multiplayer server).
 */
public class RandomBlocksClientGameTest implements FabricClientGameTest {
	private static final String[] BLOCKS = {"minecraft:stone", "minecraft:andesite", "minecraft:cobblestone"};
	private static final int BASE_X = 1000;
	private static final int Y = 100;
	private static final int BASE_Z = 1000;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
				.create()) {
			TestServerConnection conn = sp.getConnection();
			setUpArena(ctx, sp.getServer(), conn);

			// Drive the UI: open the palette, screenshot it, press Place.
			configurePalette(ctx, Palette.Mode.RANDOM);
			ctx.setScreen(PaletteScreen::new);
			ctx.waitTicks(5);
			ctx.takeScreenshot("randomblocks-palette-random");
			ctx.clickScreenButton("screen.randomblocks.place");
			ctx.waitTicks(3);
			assertTrue(ctx.computeOnClient(mc -> Palette.get().active), "Place should activate the palette");
			assertHolding(ctx, "after Place, a palette block should be swapped from the inventory into the hand");

			runRandomLine(ctx, sp.getServer(), 0, "singleplayer");
			ctx.takeScreenshot("randomblocks-random-line");

			configurePalette(ctx, Palette.Mode.GRADIENT);
			ctx.setScreen(PaletteScreen::new);
			ctx.waitTicks(5);
			ctx.takeScreenshot("randomblocks-palette-gradient");
			ctx.clickScreenButton("screen.randomblocks.place");
			ctx.waitTicks(3);
			runGradientLine(ctx, sp.getServer(), 4, "singleplayer");
			ctx.takeScreenshot("randomblocks-gradient-line");
		}

		try (TestDedicatedServerContext server = ctx.worldBuilder().createServer();
				TestDedicatedServerConnection conn = server.connect()) {
			setUpArena(ctx, server, conn);

			configurePalette(ctx, Palette.Mode.RANDOM);
			ctx.runOnClient(mc -> RandomBlocksClient.start());
			ctx.waitTicks(3);
			runRandomLine(ctx, server, 0, "dedicated");

			configurePalette(ctx, Palette.Mode.GRADIENT);
			ctx.runOnClient(mc -> RandomBlocksClient.start());
			ctx.waitTicks(3);
			runGradientLine(ctx, server, 4, "dedicated");
		}
	}

	private static void setUpArena(ClientGameTestContext ctx, TestServerContext server, TestServerConnection conn) {
		server.runCommand("gamemode survival @a");
		server.runCommand("forceload add " + (BASE_X - 16) + " " + (BASE_Z - 16) + " " + (BASE_X + 80) + " " + (BASE_Z + 16));
		server.runCommand("fill " + (BASE_X - 2) + " " + (Y - 1) + " " + (BASE_Z - 2) + " "
			+ (BASE_X + 70) + " " + (Y - 1) + " " + (BASE_Z + 12) + " glass");
		server.runCommand("fill " + (BASE_X - 2) + " " + Y + " " + (BASE_Z - 2) + " "
			+ (BASE_X + 70) + " " + (Y + 3) + " " + (BASE_Z + 12) + " air");
		server.runCommand("tp @a " + BASE_X + ".5 " + Y + " " + BASE_Z + ".5");
		server.runCommand("clear @a");
		for (int i = 1; i < 9; i++) server.runCommand("item replace entity @a hotbar." + i + " with stick");
		for (int i = 0; i < BLOCKS.length; i++) {
			server.runCommand("item replace entity @a inventory." + i + " with " + BLOCKS[i] + " 64");
		}
		conn.waitForChunksDownload();
		ctx.waitTicks(10);
		ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		ctx.waitTicks(2);
	}

	private static void configurePalette(ClientGameTestContext ctx, Palette.Mode mode) {
		ctx.runOnClient(mc -> {
			Palette p = Palette.get();
			p.active = false;
			p.entries.clear();
			int[] weights = mode == Palette.Mode.RANDOM ? new int[] {50, 30, 20} : new int[] {34, 33, 33};
			for (int i = 0; i < BLOCKS.length; i++) {
				Palette.Entry e = new Palette.Entry(BLOCKS[i]);
				e.weight = weights[i];
				e.enabled = true;
				p.entries.add(e);
			}
			p.mode = mode;
			p.axis = Palette.Axis.X;
			p.gradientLength = 30;
			p.blend = 0.2;
		});
	}

	/** Places one block at (BASE_X + i, Y, BASE_Z + row + 3) by clicking the top of the glass under it. */
	private static void place(ClientGameTestContext ctx, TestServerContext server, int i, int row) {
		int x = BASE_X + i;
		int z = BASE_Z + row + 3;
		server.runCommand("tp @a " + x + ".5 " + Y + " " + (BASE_Z + row) + ".5 0 40");
		ctx.waitTicks(3);
		ctx.runOnClient(mc -> {
			BlockPos floor = new BlockPos(x, Y - 1, z);
			BlockHitResult hit = new BlockHitResult(new Vec3(x + 0.5, Y, z + 0.5), Direction.UP, floor, false);
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
		});
		ctx.waitTicks(2);
	}

	private static List<String> readLine(ClientGameTestContext ctx, TestServerContext server, int count, int row) {
		ctx.waitTicks(10);
		return server.computeOnServer(s -> {
			List<String> out = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				BlockPos pos = new BlockPos(BASE_X + i, Y, BASE_Z + row + 3);
				out.add(BuiltInRegistries.BLOCK.getKey(s.overworld().getBlockState(pos).getBlock()).toString());
			}
			return out;
		});
	}

	private static void runRandomLine(ClientGameTestContext ctx, TestServerContext server, int row, String where) {
		int n = 60;
		for (int i = 0; i < n; i++) place(ctx, server, i, row);
		List<String> line = readLine(ctx, server, n, row);
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (String b : line) counts.merge(b, 1, Integer::sum);
		System.out.println("[randomblocks-test] " + where + " random 50/30/20 over " + n + ": " + counts);

		assertTrue(!counts.containsKey("minecraft:air"), where + ": every placement should land on the server, got " + counts);
		for (String b : BLOCKS) assertTrue(counts.getOrDefault(b, 0) > 0, where + ": expected some " + b + " in " + counts);
		int stone = counts.getOrDefault("minecraft:stone", 0);
		assertTrue(stone >= 18 && stone <= 42, where + ": stone should be about 50%, got " + counts);

		int used = ctx.computeOnClient(mc -> {
			Inventory inv = mc.player.getInventory();
			int left = 0;
			for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
				String id = BuiltInRegistries.ITEM.getKey(inv.getItem(i).getItem()).toString();
				for (String b : BLOCKS) if (b.equals(id)) left += inv.getItem(i).getCount();
			}
			return 64 * BLOCKS.length - left;
		});
		System.out.println("[randomblocks-test] " + where + " blocks used from inventory: " + used);
		assertTrue(used >= n, where + ": survival should consume blocks, used " + used);
	}

	private static void runGradientLine(ClientGameTestContext ctx, TestServerContext server, int row, String where) {
		int n = 30;
		for (int i = 0; i < n; i++) place(ctx, server, i, row);
		List<String> line = readLine(ctx, server, n, row);
		StringBuilder sb = new StringBuilder();
		for (String b : line) {
			sb.append(switch (b) {
				case "minecraft:stone" -> 'S';
				case "minecraft:andesite" -> 'A';
				case "minecraft:cobblestone" -> 'C';
				default -> '.';
			});
		}
		System.out.println("[randomblocks-test] " + where + " gradient S->A->C: " + sb);

		for (int i = 0; i <= 5; i++) assertEquals("minecraft:stone", line.get(i), where + " gradient start @" + i + " " + sb);
		for (int i = 14; i <= 15; i++) assertEquals("minecraft:andesite", line.get(i), where + " gradient middle @" + i + " " + sb);
		for (int i = 24; i < n; i++) assertEquals("minecraft:cobblestone", line.get(i), where + " gradient end @" + i + " " + sb);
	}

	private static void assertHolding(ClientGameTestContext ctx, String message) {
		String held = ctx.computeOnClient(mc ->
			BuiltInRegistries.ITEM.getKey(mc.player.getInventory().getSelectedItem().getItem()).toString());
		assertTrue(List.of(BLOCKS).contains(held), message + " (holding " + held + ")");
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static void assertEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + " but was " + actual);
	}
}
