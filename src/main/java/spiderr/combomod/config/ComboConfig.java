package spiderr.combomod.config;

import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.api.WalksyLibConfig;
import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.OptionDescription;
import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.ColorOption;
import main.walksy.lib.core.config.local.options.NumericalOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Holds the mod's live, mutable settings and hands WalksyLib a description of
 * how to build a config screen for them. Registered via the "walksylib"
 * entrypoint in fabric.mod.json, which is how it shows up in ModMenu
 * automatically (WalksyLib supplies its own ModMenuApi implementation that
 * looks up every mod registered this way — we don't need our own).
 */
public class ComboConfig implements WalksyLibApi, WalksyLibConfig {

	// ---- Combo counter settings ----
	public static boolean enabled = true;
	public static WalksyLibColor textColor = new WalksyLibColor(255, 255, 255);
	public static int timeoutTicks = 60; // 3 seconds at 20 ticks/sec
	public static int verticalOffset = 50; // pixels above the bottom of the screen
	public static boolean showAtZero = true;

	// ---- FPS counter settings ----
	public static boolean fpsCounterEnabled = true;
	public static WalksyLibColor fpsTextColor = new WalksyLibColor(255, 255, 255);

	@Override
	public LocalConfig getConfig() {
		return getOrCreateConfig();
	}

	@Override
	public LocalConfig define() {
		OptionGroup comboGroup = OptionGroup.createBuilder("Combo Counter")
				.addOption(BooleanOption.createBuilder(
						"Enabled",
						() -> enabled,
						true,
						value -> enabled = value)
					.description(OptionDescription.ofOrderedString(() -> "Turns the combo counter on or off entirely."))
					.build())
				.addOption(ColorOption.createBuilder(
						"Text Color",
						() -> textColor,
						new WalksyLibColor(255, 255, 255),
						value -> textColor = value)
					.description(OptionDescription.ofOrderedString(() -> "Color of the combo counter text."))
					.build())
				.addOption(NumericalOption.createBuilder(
						"Timeout (ticks)",
						() -> timeoutTicks,
						60,
						value -> timeoutTicks = value)
					.values(10, 200, 5)
					.description(OptionDescription.ofOrderedString(() -> "How long the combo can go without a new hit before it resets. 20 ticks = 1 second."))
					.build())
				.addOption(NumericalOption.createBuilder(
						"Vertical Offset",
						() -> verticalOffset,
						50,
						value -> verticalOffset = value)
					.values(10, 150, 1)
					.description(OptionDescription.ofOrderedString(() -> "How far above the bottom of the screen the counter sits."))
					.build())
				.addOption(BooleanOption.createBuilder(
						"Show At Zero",
						() -> showAtZero,
						true,
						value -> showAtZero = value)
					.description(OptionDescription.ofOrderedString(() -> "If off, the counter is hidden entirely instead of showing \"Combo: 0\" when there's no active combo."))
					.build())
				.build();

		OptionGroup fpsGroup = OptionGroup.createBuilder("FPS Counter")
				.addOption(BooleanOption.createBuilder(
						"Enabled",
						() -> fpsCounterEnabled,
						true,
						value -> fpsCounterEnabled = value)
					.description(OptionDescription.ofOrderedString(() -> "Turns the bottom-left FPS counter on or off."))
					.build())
				.addOption(ColorOption.createBuilder(
						"Text Color",
						() -> fpsTextColor,
						new WalksyLibColor(255, 255, 255),
						value -> fpsTextColor = value)
					.description(OptionDescription.ofOrderedString(() -> "Color of the FPS counter text."))
					.build())
				.build();

		Category comboCategory = Category.createBuilder("Combo Counter")
				.group(comboGroup)
				.build();

		Category fpsCategory = Category.createBuilder("FPS Counter")
				.group(fpsGroup)
				.build();

		return LocalConfig.createBuilder("PvP Misc")
				.path(FabricLoader.getInstance().getConfigDir().resolve("pvpmisc.json"))
				.category(comboCategory)
				.category(fpsCategory)
				.build();
	}
}
