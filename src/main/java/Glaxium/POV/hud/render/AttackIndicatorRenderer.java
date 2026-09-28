package Glaxium.POV.hud.render;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import java.util.List;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public final class AttackIndicatorRenderer {
   private AttackIndicatorRenderer() {
   }

   public static boolean shouldDraw() {
      AttackIndicator mode = (AttackIndicator)MinecraftClient.getInstance().options.getAttackIndicator().getValue();
      return mode == AttackIndicator.CROSSHAIR;
   }

   public static float progress(ReplayKeyframes replay, RecordedHudData hud, RecordedHandData hand, float tick) {
      return hud != null && !hud.attackCooldown.isEmpty() ? sampleCooldown(hud.attackCooldown, tick) : reconstruct(replay, hand, tick);
   }

   public static float sampleCooldown(KeyframeChannel<Float> channel, float tick) {
      if (channel != null && !channel.isEmpty()) {
         List<Keyframe<Float>> keyframes = channel.getKeyframes();
         int size = keyframes.size();
         if (tick < keyframes.get(0).getTick()) {
            return 1.0F;
         } else if (tick >= keyframes.get(size - 1).getTick()) {
            return MathHelper.clamp((Float)keyframes.get(size - 1).getValue(), 0.0F, 1.0F);
         } else {
            int prevIdx = 0;
            int i = 0;

            while (i < size && keyframes.get(i).getTick() <= tick) {
               prevIdx = i++;
            }

            Keyframe<Float> prev = keyframes.get(prevIdx);
            Keyframe<Float> next = keyframes.get(prevIdx + 1);
            float v0 = (Float)prev.getValue();
            float v1 = (Float)next.getValue();
            if (v0 >= 0.999F && v1 < v0) {
               return 1.0F;
            } else if (v1 < v0) {
               return 1.0F;
            } else {
               float t0 = prev.getTick();
               float t1 = next.getTick();
               if (t1 <= t0) {
                  return MathHelper.clamp(v1, 0.0F, 1.0F);
               } else {
                  float factor = (tick - t0) / (t1 - t0);
                  float value = v0 + (v1 - v0) * factor;
                  return MathHelper.clamp(value, 0.0F, 1.0F);
               }
            }
         }
      } else {
         return 1.0F;
      }
   }

   public static float liveProgress() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.player == null ? 1.0F : MathHelper.clamp(client.player.getAttackCooldownProgress(0.0F), 0.0F, 1.0F);
   }

   private static float reconstruct(ReplayKeyframes replay, RecordedHandData hand, float tick) {
      if (replay != null && !(tick < 0.0F)) {
         int slot = MathHelper.clamp(replay.getSelectedSlot(tick), 0, 8);
         ItemStack main = (ItemStack)((KeyframeChannel)replay.hotbar.get(slot)).interpolate(tick, ItemStack.EMPTY);
         float period = cooldownPeriod(main);
         int lookback = Math.max(2, (int)Math.ceil((double)period) + 1);

         for (int i = 0; i <= lookback; i++) {
            float thenTick = tick - (float)i;
            if (thenTick < 0.0F) {
               return 1.0F;
            }

            if (resetAt(replay, hand, thenTick)) {
               return MathHelper.clamp((float)i / period, 0.0F, 1.0F);
            }
         }

         return 1.0F;
      } else {
         return 1.0F;
      }
   }

   private static boolean resetAt(ReplayKeyframes replay, RecordedHandData hand, float tick) {
      float previousTick = tick - 1.0F;
      if (previousTick < 0.0F) {
         return false;
      } else {
         int slot = MathHelper.clamp(replay.getSelectedSlot(tick), 0, 8);
         int previousSlot = MathHelper.clamp(replay.getSelectedSlot(previousTick), 0, 8);
         if (slot != previousSlot) {
            return true;
         } else {
            ItemStack main = (ItemStack)((KeyframeChannel)replay.hotbar.get(slot)).interpolate(tick, ItemStack.EMPTY);
            ItemStack previousMain = (ItemStack)((KeyframeChannel)replay.hotbar.get(previousSlot)).interpolate(previousTick, ItemStack.EMPTY);
            if (!ItemStack.areItemsEqual(main, previousMain)) {
               return true;
            } else {
               ItemStack off = (ItemStack)replay.offHand.interpolate(tick, ItemStack.EMPTY);
               ItemStack previousOff = (ItemStack)replay.offHand.interpolate(previousTick, ItemStack.EMPTY);
               return !ItemStack.areItemsEqual(off, previousOff);
            }
         }
      }
   }

   private static float cooldownPeriod(ItemStack stack) {
      double base = 4.0;
      double addition = 0.0;
      double multiplyBase = 1.0;
      double multiplyTotal = 1.0;
      if (stack != null && !stack.isEmpty()) {
         for (EntityAttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
            switch (modifier.getOperation()) {
               case ADDITION:
                  addition += modifier.getValue();
                  break;
               case MULTIPLY_BASE:
                  multiplyBase += modifier.getValue();
                  break;
               case MULTIPLY_TOTAL:
                  multiplyTotal *= 1.0 + modifier.getValue();
            }
         }
      }

      double speed = Math.max(1.0E-4, (base + addition) * multiplyBase * multiplyTotal);
      return (float)(1.0 / speed * 20.0);
   }
}
