package Glaxium.POV.hud.playback;

import Glaxium.POV.hud.HudState;
import Glaxium.POV.hud.RecordedHudData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

/** Samples HUD channels into a frame-local {@link HudState}. */
public final class HudSampler
{
    private static final float MAX_HEALTH_CONTAINER = 1200F;
    private static final int ABSORPTION_FLASH_WINDOW_TICKS = 10;
    private static final int HEALTH_STABLE_TICKS = 20;

    private HudSampler()
    {
    }

    /** Samples the selected actor's POV tracks at an absolute replay tick. */
    public static HudState sample(RecordedHudData data, ReplayKeyframes replay, float tick)
    {
        if (data == null)
        {
            return null;
        }

        HudState state = new HudState();

        state.visible = data.visible.interpolate(tick, true);
        state.statusBarsVisible = data.statusBarsVisible.interpolate(tick, true);
        /* An empty Crosshair track means the normal POV default: visible.
         * The editor already seeds the first explicitly authored key with true,
         * so adding/removing that first key no longer changes the apparent
         * default state. Users can insert a false key wherever it should hide. */
        state.crosshair = data.crosshair.interpolate(tick, true);
        state.selectedSlot = clamp(replay.getSelectedSlot(tick), 0, ReplayKeyframes.HOTBAR_SIZE - 1);

        for (int i = 0; i < ReplayKeyframes.HOTBAR_SIZE; i++)
        {
            state.items[i] = copyItem(replay.hotbar.get(i).interpolate(tick, ItemStack.EMPTY));
        }

        state.offhandItem = copyItem(replay.offHand.interpolate(tick, ItemStack.EMPTY));
        state.healthContainer = clamp(data.healthContainer.interpolate(tick, 20), 0F, MAX_HEALTH_CONTAINER);
        state.health = clamp(data.health.interpolate(tick, 20), 0F, state.healthContainer);
        state.lastHealth = sampleHealth(data, tick - 1F, state.health);

        float[] recentHealth = recentRange(
            data.health,
            data.healthContainer,
            tick,
            state.health,
            HEALTH_STABLE_TICKS);
        state.recentHealthLow = recentHealth[0];
        state.recentHealthHigh = recentHealth[1];
        /* Old films have no previous-health keys. Their recent high value is the closest
         * deterministic reconstruction of the pre-damage health already available. */
        state.previousHealth = data.previousHealth.isEmpty()
            ? clamp(state.recentHealthHigh, 0F, state.healthContainer)
            : clamp(data.previousHealth.interpolate(tick, Math.round(state.recentHealthHigh)), 0F, state.healthContainer);
        state.absorptionContainer = clamp(data.absorptionContainer.interpolate(tick, 0), 0F, MAX_HEALTH_CONTAINER);
        state.absorption = clamp(data.absorption.interpolate(tick, 0), 0F, state.absorptionContainer);

        float[] recentAbsorption = recentRange(
            data.absorption,
            data.absorptionContainer,
            tick,
            state.absorption,
            ABSORPTION_FLASH_WINDOW_TICKS);
        state.recentAbsorptionLow = recentAbsorption[0];
        state.recentAbsorptionHigh = recentAbsorption[1];
        state.heartType = clamp(data.heartType.interpolate(tick, HudState.HEART_NORMAL), HudState.HEART_NORMAL, HudState.HEART_FROZEN);
        state.hardcore = data.hardcore.interpolate(tick, false);
        state.heartRegeneration = data.regeneration.interpolate(tick, false);
        state.armor = clamp(data.armor.interpolate(tick, 0), 0F, 20F);
        state.hunger = clamp(data.hunger.interpolate(tick, 20), 0F, 20F);
        state.hungerEffect = data.hungerEffect.interpolate(tick, false);
        state.mountHealthContainer = clamp(data.mountHealthContainer.interpolate(tick, 0), 0F, 60F);
        state.mountHealth = state.mountHealthContainer <= 0F
            ? 0F
            : clamp(data.mountHealth.interpolate(tick, 0), 0F, state.mountHealthContainer);
        state.air = clamp(data.air.interpolate(tick, 300), 0F, 300F);
        state.experience = clamp(data.experience.interpolate(tick, 0D).floatValue(), 0F, 1F);
        state.experienceLevel = clamp(data.experienceLevel.interpolate(tick, 0), 0, 9999);
        state.heartFlash = data.heartFlash.interpolate(tick, false);
        state.absorptionFlash = data.absorptionFlash.interpolate(tick, false);
        state.healthFlashAge = state.heartFlash ? getTrueRunAge(data.heartFlash, tick) : 0F;

        Transform transform = data.layout.interpolate(tick, new Transform());
        state.layout.copy(transform);
        state.slotsLayout.copy(data.slotsLayout.interpolate(tick, new Transform()));
        state.heartsLayout.copy(data.heartsLayout.interpolate(tick, new Transform()));
        state.foodLayout.copy(data.foodLayout.interpolate(tick, new Transform()));
        state.expLayout.copy(data.expLayout.interpolate(tick, new Transform()));
        state.cursorVisible = data.cursorVisible.interpolate(tick, false);
        state.cursorLayout.copy(data.cursorLayout.interpolate(tick, new Transform()));
        state.cursorItem = copyItem(data.cursorItem.interpolate(tick, ItemStack.EMPTY));
        state.alpha = 1F;

        return state;
    }

    private static float sampleHealth(RecordedHudData data, float tick, float fallback)
    {
        float container = clamp(data.healthContainer.interpolate(tick, 20), 0F, MAX_HEALTH_CONTAINER);
        return clamp(data.health.interpolate(tick, Math.round(fallback)), 0F, container);
    }

    private static float[] recentRange(
        KeyframeChannel<Integer> value,
        KeyframeChannel<Integer> container,
        float tick,
        float current,
        int windowTicks)
    {
        float low = current;
        float high = current;

        for (int i = 1; i <= windowTicks; i++)
        {
            float maximum = clamp(container.interpolate(tick - i, 0), 0F, MAX_HEALTH_CONTAINER);
            float sampled = clamp(value.interpolate(tick - i, 0), 0F, maximum);
            low = Math.min(low, sampled);
            high = Math.max(high, sampled);
        }

        return new float[] {low, high};
    }

    /** Finds the start of the active true segment without mutable playback state. */
    private static float getTrueRunAge(KeyframeChannel<Boolean> channel, float tick)
    {
        boolean previous = false;
        float start = tick;

        for (Keyframe<Boolean> keyframe : channel.getKeyframes())
        {
            if (keyframe.getTick() > tick)
            {
                break;
            }

            boolean value = Boolean.TRUE.equals(keyframe.getValue());

            if (value && !previous)
            {
                start = keyframe.getTick();
            }

            previous = value;
        }

        return previous ? Math.max(0F, tick - start) : 0F;
    }

    private static ItemStack copyItem(ItemStack stack)
    {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
