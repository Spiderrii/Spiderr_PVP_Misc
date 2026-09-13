package spiderr.combomod.combo;

import spiderr.combomod.config.ComboConfig;

/**
 * Tracks the player's current combo count.
 *
 * A hit on an entity increases the combo and resets the idle timer.
 * The combo resets to zero if the player misses a swing, takes damage,
 * or goes too long without landing another hit.
 */
public final class ComboManager {
	private ComboManager() {
	}

	private static int comboCount = 0;
	private static int ticksSinceLastHit = 0;

	public static void onHit() {
		comboCount++;
		ticksSinceLastHit = 0;
	}

	public static void onMiss() {
		reset();
	}

	public static void onDamageTaken() {
		reset();
	}

	public static void reset() {
		comboCount = 0;
		ticksSinceLastHit = 0;
	}

	/**
	 * Call once per client tick to advance the idle timer and expire the combo if needed.
	 */
	public static void tick() {
		if (comboCount == 0) {
			return;
		}

		ticksSinceLastHit++;
		if (ticksSinceLastHit >= ComboConfig.timeoutTicks) {
			reset();
		}
	}

	public static int getCount() {
		return comboCount;
	}
}
