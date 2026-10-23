package dev.aadit.randomblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class BlockPickerTest {
	private static final boolean[] ALL = {true, true, true};

	@Test
	void weightedPickFollowsPercentages() {
		int[] weights = {50, 30, 20};
		int[] counts = new int[3];
		Random rng = new Random(42);
		int n = 100_000;
		for (int i = 0; i < n; i++) counts[BlockPicker.pickWeighted(weights, ALL, rng)]++;

		assertEquals(0.50, counts[0] / (double) n, 0.01);
		assertEquals(0.30, counts[1] / (double) n, 0.01);
		assertEquals(0.20, counts[2] / (double) n, 0.01);
	}

	@Test
	void weightedPickSkipsUnavailable() {
		int[] weights = {50, 30, 20};
		boolean[] available = {false, true, true};
		Random rng = new Random(1);
		for (int i = 0; i < 10_000; i++) {
			assertTrue(BlockPicker.pickWeighted(weights, available, rng) != 0);
		}
		assertEquals(-1, BlockPicker.pickWeighted(weights, new boolean[3], rng));
	}

	@Test
	void gradientBandsFollowWidthsWithoutBlend() {
		int[] weights = {50, 30, 20};
		Random rng = new Random(7);
		assertEquals(0, BlockPicker.pickGradient(weights, ALL, 0.0, 0, rng));
		assertEquals(0, BlockPicker.pickGradient(weights, ALL, 0.49, 0, rng));
		assertEquals(1, BlockPicker.pickGradient(weights, ALL, 0.51, 0, rng));
		assertEquals(1, BlockPicker.pickGradient(weights, ALL, 0.79, 0, rng));
		assertEquals(2, BlockPicker.pickGradient(weights, ALL, 0.81, 0, rng));
		assertEquals(2, BlockPicker.pickGradient(weights, ALL, 1.0, 0, rng));
	}

	@Test
	void gradientBlendMixesNeighboursAtEdges() {
		int[] weights = {50, 50};
		boolean[] available = {true, true};
		Random rng = new Random(3);
		int[] counts = new int[2];
		for (int i = 0; i < 10_000; i++) counts[BlockPicker.pickGradient(weights, available, 0.5, 0.4, rng)]++;
		assertEquals(0.5, counts[0] / 10_000.0, 0.03);
	}

	@Test
	void gradientFallsBackToNearestAvailableBand() {
		int[] weights = {34, 33, 33};
		boolean[] available = {true, false, true};
		Random rng = new Random(9);
		int pick = BlockPicker.pickGradient(weights, available, 0.5, 0, rng);
		assertTrue(pick == 0 || pick == 2);
	}

	@Test
	void gradientTClampsAlongAxis() {
		assertEquals(0.0, BlockPicker.gradientT(0, 0, 0, 1, 0, 0, 11));
		assertEquals(0.5, BlockPicker.gradientT(5, 3, 0, 1, 0, 0, 11));
		assertEquals(1.0, BlockPicker.gradientT(20, 0, 0, 1, 0, 0, 11));
		assertEquals(0.0, BlockPicker.gradientT(-4, 0, 0, 1, 0, 0, 11));
		assertEquals(0.5, BlockPicker.gradientT(0, 0, -5, 0, 0, -1, 11));
	}
}
