package dev.aadit.randomblocks;

import java.util.Random;

/**
 * Pure selection logic, kept free of Minecraft types so it can be unit tested.
 * Entries are addressed by index; {@code weights[i]} is the user's percentage and
 * {@code available[i]} says whether the player still has any of that block.
 */
public final class BlockPicker {
	private BlockPicker() {
	}

	/** Weighted random pick. Returns -1 when nothing is available. */
	public static int pickWeighted(int[] weights, boolean[] available, Random rng) {
		int total = 0;
		for (int i = 0; i < weights.length; i++) {
			if (available[i] && weights[i] > 0) total += weights[i];
		}
		if (total <= 0) return -1;

		int roll = rng.nextInt(total);
		for (int i = 0; i < weights.length; i++) {
			if (!available[i] || weights[i] <= 0) continue;
			roll -= weights[i];
			if (roll < 0) return i;
		}
		return -1;
	}

	/**
	 * Gradient pick. Entries are laid out in order along [0, 1], each band as wide as its
	 * share of the total weight. {@code t} is jittered by up to ±blend/2 so band edges dither
	 * into each other. If the chosen band has run out, the nearest band that is still
	 * available is used instead, so the rest of the gradient keeps its shape.
	 */
	public static int pickGradient(int[] weights, boolean[] available, double t, double blend, Random rng) {
		int total = 0;
		for (int w : weights) total += Math.max(0, w);
		if (total <= 0) return -1;

		double jittered = t + (rng.nextDouble() - 0.5) * blend;
		jittered = Math.clamp(jittered, 0.0, Math.nextDown(1.0));

		int band = weights.length - 1;
		double edge = 0;
		for (int i = 0; i < weights.length; i++) {
			edge += Math.max(0, weights[i]) / (double) total;
			if (jittered < edge) {
				band = i;
				break;
			}
		}
		return nearestAvailable(weights, available, band);
	}

	/** Position of {@code offset} along {@code axis}, as a fraction of {@code length}, clamped to [0, 1]. */
	public static double gradientT(int dx, int dy, int dz, int ax, int ay, int az, int length) {
		if (length <= 1) return 0;
		double along = dx * ax + dy * ay + dz * az;
		return Math.clamp(along / (length - 1), 0.0, 1.0);
	}

	private static int nearestAvailable(int[] weights, boolean[] available, int band) {
		for (int d = 0; d < weights.length; d++) {
			int lo = band - d;
			int hi = band + d;
			if (lo >= 0 && available[lo] && weights[lo] > 0) return lo;
			if (hi < weights.length && available[hi] && weights[hi] > 0) return hi;
		}
		return -1;
	}
}
