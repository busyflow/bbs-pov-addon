package Glaxium.POV.hud.playback;

import Glaxium.POV.hud.HudState;
import Glaxium.POV.hud.RecordedHudData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public final class HudSampler {
   private static final float MAX_HEALTH_CONTAINER = 1200.0F;
   private static final int ABSORPTION_FLASH_WINDOW_TICKS = 10;
   private static final int HEALTH_STABLE_TICKS = 20;

   private HudSampler() {
   }

   public static HudState sample(RecordedHudData data, ReplayKeyframes replay, float tick) {
      if (!data.hasRecordedData() && data.crosshair.isEmpty()) {
         return null;
      } else {
         HudState state = new HudState();
         state.visible = (Boolean)data.visible.interpolate(tick, true);
         state.statusBarsVisible = (Boolean)data.statusBarsVisible.interpolate(tick, true);
         state.crosshair = (Boolean)data.crosshair.interpolate(tick, true);
         state.selectedSlot = clamp(replay.getSelectedSlot(tick), 0, 8);

         for (int i = 0; i < 9; i++) {
            state.items[i] = copyItem((ItemStack)((KeyframeChannel)replay.hotbar.get(i)).interpolate(tick, ItemStack.EMPTY));
         }

         state.offhandItem = copyItem((ItemStack)replay.offHand.interpolate(tick, ItemStack.EMPTY));
         state.healthContainer = clamp((float)((Integer)data.healthContainer.interpolate(tick, 20)).intValue(), 0.0F, 1200.0F);
         state.health = clamp((float)((Integer)data.health.interpolate(tick, 20)).intValue(), 0.0F, state.healthContainer);
         state.lastHealth = sampleHealth(data, tick - 1.0F, state.health);
         float[] recentHealth = recentRange(data.health, data.healthContainer, tick, state.health, 20);
         state.recentHealthLow = recentHealth[0];
         state.recentHealthHigh = recentHealth[1];
         state.previousHealth = data.previousHealth.isEmpty()
            ? clamp(state.recentHealthHigh, 0.0F, state.healthContainer)
            : clamp((float)((Integer)data.previousHealth.interpolate(tick, Math.round(state.recentHealthHigh))).intValue(), 0.0F, state.healthContainer);
         state.absorptionContainer = clamp((float)((Integer)data.absorptionContainer.interpolate(tick, 0)).intValue(), 0.0F, 1200.0F);
         state.absorption = clamp((float)((Integer)data.absorption.interpolate(tick, 0)).intValue(), 0.0F, state.absorptionContainer);
         float[] recentAbsorption = recentRange(data.absorption, data.absorptionContainer, tick, state.absorption, 10);
         state.recentAbsorptionLow = recentAbsorption[0];
         state.recentAbsorptionHigh = recentAbsorption[1];
         state.heartType = clamp((Integer)data.heartType.interpolate(tick, 0), 0, 4);
         state.hardcore = (Boolean)data.hardcore.interpolate(tick, false);
         state.heartRegeneration = (Boolean)data.regeneration.interpolate(tick, false);
         state.armor = clamp((float)((Integer)data.armor.interpolate(tick, 0)).intValue(), 0.0F, 20.0F);
         state.hunger = clamp((float)((Integer)data.hunger.interpolate(tick, 20)).intValue(), 0.0F, 20.0F);
         state.hungerEffect = (Boolean)data.hungerEffect.interpolate(tick, false);
         state.mountHealthContainer = clamp((float)((Integer)data.mountHealthContainer.interpolate(tick, 0)).intValue(), 0.0F, 60.0F);
         state.mountHealth = state.mountHealthContainer <= 0.0F
            ? 0.0F
            : clamp((float)((Integer)data.mountHealth.interpolate(tick, 0)).intValue(), 0.0F, state.mountHealthContainer);
         state.air = clamp((float)((Integer)data.air.interpolate(tick, 300)).intValue(), 0.0F, 300.0F);
         state.experience = clamp(((Double)data.experience.interpolate(tick, 0.0)).floatValue(), 0.0F, 1.0F);
         state.experienceLevel = clamp((Integer)data.experienceLevel.interpolate(tick, 0), 0, 9999);
         state.heartFlash = (Boolean)data.heartFlash.interpolate(tick, false);
         state.absorptionFlash = (Boolean)data.absorptionFlash.interpolate(tick, false);
         state.healthFlashAge = state.heartFlash ? getTrueRunAge(data.heartFlash, tick) : 0.0F;
         Transform transform = (Transform)data.layout.interpolate(tick, new Transform());
         state.layout.copy(transform);
         state.cursorVisible = (Boolean)data.cursorVisible.interpolate(tick, false);
         state.cursorLayout.copy((Transform)data.cursorLayout.interpolate(tick, new Transform()));
         state.cursorItem = copyItem((ItemStack)data.cursorItem.interpolate(tick, ItemStack.EMPTY));
         state.alpha = 1.0F;
         return state;
      }
   }

   private static float sampleHealth(RecordedHudData data, float tick, float fallback) {
      float container = clamp((float)((Integer)data.healthContainer.interpolate(tick, 20)).intValue(), 0.0F, 1200.0F);
      return clamp((float)((Integer)data.health.interpolate(tick, Math.round(fallback))).intValue(), 0.0F, container);
   }

   private static float[] recentRange(KeyframeChannel<Integer> value, KeyframeChannel<Integer> container, float tick, float current, int windowTicks) {
      float low = current;
      float high = current;

      for (int i = 1; i <= windowTicks; i++) {
         float maximum = clamp((float)((Integer)container.interpolate(tick - (float)i, 0)).intValue(), 0.0F, 1200.0F);
         float sampled = clamp((float)((Integer)value.interpolate(tick - (float)i, 0)).intValue(), 0.0F, maximum);
         low = Math.min(low, sampled);
         high = Math.max(high, sampled);
      }

      return new float[]{low, high};
   }

   private static float getTrueRunAge(KeyframeChannel<Boolean> channel, float tick) {
      boolean previous = false;
      float start = tick;

      for (Keyframe<Boolean> keyframe : channel.getKeyframes()) {
         if (keyframe.getTick() > tick) {
            break;
         }

         boolean value = Boolean.TRUE.equals(keyframe.getValue());
         if (value && !previous) {
            start = keyframe.getTick();
         }

         previous = value;
      }

      return previous ? Math.max(0.0F, tick - start) : 0.0F;
   }

   private static ItemStack copyItem(ItemStack stack) {
      return stack == null ? ItemStack.EMPTY : stack.copy();
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
