package dev.aadit.randomblocks;

import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small strip in the top-left corner showing the active palette while placement is on. */
public final class PaletteHud {
	private static final int MAX_ICONS = 9;

	private PaletteHud() {
	}

	public static void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Palette palette = Palette.get();
		Minecraft mc = Minecraft.getInstance();
		if (!palette.active) return;

		List<Palette.Entry> entries = palette.enabledEntries();
		int total = 0;
		for (Palette.Entry e : entries) total += e.weight;

		boolean gradient = palette.mode == Palette.Mode.GRADIENT;
		String label = gradient ? "Gradient →" : "Random";
		g.text(mc.font, label, 4, 4, 0xFFFFFF55, true);

		int x = 4;
		int y = 15;
		int shown = Math.min(entries.size(), MAX_ICONS);
		for (int i = 0; i < shown; i++) {
			Palette.Entry e = entries.get(i);
			Item item = e.resolve();
			g.fill(x - 1, y - 1, x + 17, y + 17, 0x66000000);
			g.item(new ItemStack(item), x, y);
			String pct = Math.round(100.0 * e.weight / total) + "%";
			g.text(mc.font, pct, x + 8 - mc.font.width(pct) / 2, y + 19, 0xFFFFFFFF, true);
			x += 22;
		}
		if (entries.size() > shown) g.text(mc.font, "+" + (entries.size() - shown), x, y + 4, 0xFFAAAAAA, true);
	}
}
