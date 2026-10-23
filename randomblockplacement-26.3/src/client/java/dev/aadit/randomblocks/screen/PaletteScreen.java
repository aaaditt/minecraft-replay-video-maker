package dev.aadit.randomblocks.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;
import java.util.function.DoubleConsumer;

import dev.aadit.randomblocks.Palette;
import dev.aadit.randomblocks.RandomBlocksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Lists every block in the hotbar and inventory. Tick the ones to use, set each one's
 * percentage, reorder them for the gradient, then press Place.
 */
public class PaletteScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int LIST_TOP = 40;
	private static final int ROW_WIDTH = 310;

	private record Row(Palette.Entry entry, ItemStack icon, int count) {
	}

	private final Palette palette = Palette.get();
	private final List<Row> rows = new ArrayList<>();
	private int scroll;

	public PaletteScreen() {
		super(Component.translatable("screen.randomblocks.title"));
		collectRows();
	}

	/** Gathers the distinct block items the player has, in palette order, adding new ones to the palette. */
	private void collectRows() {
		Map<Item, Integer> counts = new LinkedHashMap<>();
		Inventory inv = Minecraft.getInstance().player.getInventory();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() instanceof BlockItem) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		for (Item item : counts.keySet()) {
			String id = BuiltInRegistries.ITEM.getKey(item).toString();
			if (palette.find(id) == null) palette.entries.add(new Palette.Entry(id));
		}
		rows.clear();
		for (Palette.Entry e : palette.entries) {
			Item item = e.resolve();
			Integer count = item == null ? null : counts.get(item);
			if (count != null) rows.add(new Row(e, new ItemStack(item), count));
		}
	}

	private int visibleRows() {
		return Math.max(1, (height - 92 - LIST_TOP) / ROW_HEIGHT);
	}

	private int left() {
		return (width - ROW_WIDTH) / 2;
	}

	@Override
	protected void init() {
		scroll = Math.clamp(scroll, 0, Math.max(0, rows.size() - visibleRows()));
		int x = left();

		int end = Math.min(rows.size(), scroll + visibleRows());
		for (int i = scroll; i < end; i++) {
			Row row = rows.get(i);
			int y = LIST_TOP + (i - scroll) * ROW_HEIGHT;
			int index = i;
			addRenderableWidget(Checkbox.builder(Component.empty(), font)
				.pos(x, y + 2)
				.selected(row.entry.enabled)
				.onValueChange((box, value) -> row.entry.enabled = value)
				.build());
			addRenderableWidget(new ValueSlider(x + 170, y, 96, (row.entry.weight - 1) / 99.0,
				v -> row.entry.weight = 1 + (int) Math.round(v * 99),
				v -> Component.literal((1 + (int) Math.round(v * 99)) + "%")));
			Button up = addRenderableWidget(Button.builder(Component.literal("▲"), b -> move(index, -1))
				.bounds(x + 268, y, 20, 20).build());
			Button down = addRenderableWidget(Button.builder(Component.literal("▼"), b -> move(index, 1))
				.bounds(x + 290, y, 20, 20).build());
			up.active = index > 0;
			down.active = index < rows.size() - 1;
		}

		int footerY = height - 76;
		int fx = width / 2 - 191;
		addRenderableWidget(Button.builder(modeLabel(), b -> {
			palette.mode = palette.mode == Palette.Mode.RANDOM ? Palette.Mode.GRADIENT : Palette.Mode.RANDOM;
			rebuildWidgets();
		}).bounds(fx, footerY, 110, 20).build());

		if (palette.mode == Palette.Mode.GRADIENT) {
			addRenderableWidget(Button.builder(axisLabel(), b -> {
				Palette.Axis[] all = Palette.Axis.values();
				palette.axis = all[(palette.axis.ordinal() + 1) % all.length];
				b.setMessage(axisLabel());
			}).bounds(fx + 114, footerY, 64, 20).build());
			addRenderableWidget(new ValueSlider(fx + 182, footerY, 98, (palette.gradientLength - 2) / 62.0,
				v -> palette.gradientLength = 2 + (int) Math.round(v * 62),
				v -> Component.translatable("screen.randomblocks.length", 2 + (int) Math.round(v * 62))));
			addRenderableWidget(new ValueSlider(fx + 284, footerY, 98, palette.blend,
				v -> palette.blend = v,
				v -> Component.translatable("screen.randomblocks.blend", (int) Math.round(v * 100))));
		}

		int by = height - 28;
		addRenderableWidget(Button.builder(Component.translatable("screen.randomblocks.place"), b -> {
			palette.save();
			onClose();
			RandomBlocksClient.start();
		}).bounds(width / 2 - 154, by, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.randomblocks.stop"), b -> {
			RandomBlocksClient.stop();
			onClose();
		}).bounds(width / 2 - 50, by, 100, 20).build()).active = palette.active;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(width / 2 + 54, by, 100, 20).build());
	}

	private Component modeLabel() {
		return Component.translatable("screen.randomblocks.mode." + palette.mode.name().toLowerCase());
	}

	private Component axisLabel() {
		return Component.translatable("screen.randomblocks.axis", palette.axis.name());
	}

	private void move(int rowIndex, int delta) {
		int other = rowIndex + delta;
		if (other < 0 || other >= rows.size()) return;
		int a = palette.entries.indexOf(rows.get(rowIndex).entry);
		int b = palette.entries.indexOf(rows.get(other).entry);
		Palette.Entry tmp = palette.entries.get(a);
		palette.entries.set(a, palette.entries.get(b));
		palette.entries.set(b, tmp);
		Row r = rows.get(rowIndex);
		rows.set(rowIndex, rows.get(other));
		rows.set(other, r);
		if (other < scroll) scroll = other;
		if (other >= scroll + visibleRows()) scroll = other - visibleRows() + 1;
		rebuildWidgets();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
		int before = scroll;
		scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, rows.size() - visibleRows()));
		if (scroll != before) rebuildWidgets();
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		g.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		g.centeredText(font, Component.translatable("screen.randomblocks.hint"), width / 2, 24, 0xFFA0A0A0);

		int x = left();
		if (rows.isEmpty()) {
			g.centeredText(font, Component.translatable("screen.randomblocks.empty"), width / 2, LIST_TOP + 20, 0xFFFF7777);
		}
		int end = Math.min(rows.size(), scroll + visibleRows());
		int total = 0;
		for (Palette.Entry e : palette.enabledEntries()) total += e.weight;
		for (int i = scroll; i < end; i++) {
			Row row = rows.get(i);
			int y = LIST_TOP + (i - scroll) * ROW_HEIGHT;
			g.item(row.icon, x + 24, y + 2);
			int color = row.entry.enabled ? 0xFFFFFFFF : 0xFF808080;
			String name = font.plainSubstrByWidth(row.icon.getHoverName().getString(), 96);
			g.text(font, name, x + 44, y + 2, color, true);
			String sub = "x" + row.count;
			if (row.entry.enabled && total > 0) sub += "  → " + Math.round(100.0 * row.entry.weight / total) + "%";
			g.text(font, sub, x + 44, y + 12, 0xFFA0A0A0, true);
		}
		if (rows.size() > visibleRows()) {
			String page = (scroll + 1) + "-" + end + " / " + rows.size();
			g.text(font, page, x + ROW_WIDTH - font.width(page), LIST_TOP - 10, 0xFFA0A0A0, true);
		}

		Component summary = total > 0
			? Component.translatable("screen.randomblocks.total", palette.enabledEntries().size(), total)
			: Component.translatable("screen.randomblocks.none_selected");
		g.centeredText(font, summary, width / 2, height - 46, total == 100 || total == 0 ? 0xFFFFFFFF : 0xFFFFDD55);
	}

	@Override
	public void onClose() {
		palette.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class ValueSlider extends AbstractSliderButton {
		private final DoubleConsumer apply;
		private final DoubleFunction<Component> label;

		ValueSlider(int x, int y, int width, double value, DoubleConsumer apply, DoubleFunction<Component> label) {
			super(x, y, width, 20, Component.empty(), Math.clamp(value, 0.0, 1.0));
			this.apply = apply;
			this.label = label;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(label.apply(value));
		}

		@Override
		protected void applyValue() {
			apply.accept(value);
		}
	}
}
