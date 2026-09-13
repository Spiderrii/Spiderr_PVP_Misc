package spiderr.combomod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import spiderr.combomod.combo.ComboManager;
import spiderr.combomod.config.ComboConfig;

public class ComboModClient implements ClientModInitializer {

	// Flip to true if you ever need to see hit/miss/damage events called out
	// in the action bar again while debugging.
	private static final boolean DEBUG = false;

	// Tracked frame-to-frame so we can detect a fresh click without consuming it
	// (isPressed() just reads key state; it doesn't eat the click like wasPressed() does,
	// so the game's own attack handling is untouched).
	private boolean wasAttackKeyPressed = false;
	private float lastKnownHealth = -1.0f;

	// Set directly by the attack callback (which fires earlier in the same tick, before
	// knockback can move anything), and also used to de-duplicate: some combat-related
	// mods can cause the attack callback to fire more than once for a single swing, so
	// only the first hit within a given tick is ever counted.
	private boolean hitLandedThisTick = false;

	@Override
	public void onInitializeClient() {
		// Landing a hit on another PLAYER extends the combo (once per tick, see hitLandedThisTick).
		// Hitting a mob/animal doesn't grow the combo, but it also isn't treated as a miss —
		// it's simply ignored, so the combo continues as if nothing happened.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!hitLandedThisTick) {
				hitLandedThisTick = true;
				if (ComboConfig.enabled && entity instanceof PlayerEntity) {
					ComboManager.onHit();
					debugMessage("HIT -> combo=" + ComboManager.getCount());
				}
			}
			return ActionResult.PASS;
		});

		// Advance the timeout timer and watch for misses / damage taken once per tick.
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		// Register our own HUD layers.
		HudElementRegistry.addLast(Identifier.of(Combomod.MOD_ID, "combo_counter"), this::renderCombo);
		HudElementRegistry.addLast(Identifier.of(Combomod.MOD_ID, "fps_counter"), this::renderFps);
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) {
			ComboManager.reset();
			wasAttackKeyPressed = false;
			lastKnownHealth = -1.0f;
			hitLandedThisTick = false;
			return;
		}

		if (ComboConfig.enabled) {
			ComboManager.tick();
			checkForMissedSwing(client);
			checkForDamageTaken(client);
		}

		// Clear this only after everything above has had a chance to read it.
		hitLandedThisTick = false;
	}

	private void checkForMissedSwing(MinecraftClient client) {
		boolean isPressed = client.options.attackKey.isPressed();

		// Only act on the rising edge (the moment the click happens), not every tick it's held.
		if (isPressed && !wasAttackKeyPressed && !hitLandedThisTick) {
			ComboManager.onMiss();
			debugMessage("MISS -> combo reset");
		}

		wasAttackKeyPressed = isPressed;
	}

	private void checkForDamageTaken(MinecraftClient client) {
		float currentHealth = client.player.getHealth();

		if (lastKnownHealth >= 0 && currentHealth < lastKnownHealth) {
			ComboManager.onDamageTaken();
			debugMessage("DAMAGE TAKEN -> combo reset");
		}

		lastKnownHealth = currentHealth;
	}

	private void renderCombo(DrawContext drawContext, RenderTickCounter tickCounter) {
		if (!ComboConfig.enabled) {
			return;
		}

		int count = ComboManager.getCount();
		if (count == 0 && !ComboConfig.showAtZero) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		String message = "Combo: " + count;
		int textWidth = client.textRenderer.getWidth(message);

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		int x = (screenWidth - textWidth) / 2;
		int y = screenHeight - ComboConfig.verticalOffset;

		drawContext.drawTextWithShadow(client.textRenderer, message, x, y, ComboConfig.textColor.getRGB());
	}

	private void renderFps(DrawContext drawContext, RenderTickCounter tickCounter) {
		if (!ComboConfig.fpsCounterEnabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.hudHidden) {
			return;
		}

		String message = client.getCurrentFps() + " fps";

		int x = 2;
		int y = client.getWindow().getScaledHeight() - 10;

		drawContext.drawTextWithShadow(client.textRenderer, message, x, y, ComboConfig.fpsTextColor.getRGB());
	}

	private void debugMessage(String message) {
		if (!DEBUG) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.sendMessage(net.minecraft.text.Text.literal("[pvpmisc] " + message), true);
		}
		Combomod.LOGGER.info("[pvpmisc] {}", message);
	}
}
