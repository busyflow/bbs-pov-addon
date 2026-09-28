package Glaxium.POV.hud;

import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.recording.HudRecorder;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.ui.ValueStringKeys;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public final class RecordedHudData {
   public final KeyframeChannel<Integer> health = channel("health", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> previousHealth = channel("previous_health", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> healthContainer = channel("health_container", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> absorption = channel("absorption", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> absorptionContainer = channel("absorption_container", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> heartType = channel("heart_type", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> hardcore = channel("hardcore", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> regeneration = channel("regeneration", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> armor = channel("armor", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> hunger = channel("hunger", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> hungerEffect = channel("hunger_effect", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> mountHealth = channel("mount_health", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> mountHealthContainer = channel("mount_health_container", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> air = channel("air", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Double> experience = channel("experience", KeyframeFactories.DOUBLE);
   public final KeyframeChannel<Integer> experienceLevel = channel("experience_level", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> heartFlash = channel("heart_flash", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> attackCooldown = channel("attack_cooldown", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> inventoryAnchor = channel("inventory_slots", KeyframeFactories.BOOLEAN);
   public final List<KeyframeChannel<ItemStack>> inventory = new ArrayList<>();
   public final KeyframeChannel<Boolean> absorptionFlash = channel("absorption_flash", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Transform> layout = channel("layout", KeyframeFactories.TRANSFORM);
   public final KeyframeChannel<Boolean> visible = channel("visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> statusBarsVisible = channel("status_bars_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> crosshair = channel("crosshair", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Transform> cursorLayout = channel("cursor_layout", KeyframeFactories.TRANSFORM);
   public final KeyframeChannel<Boolean> cursorVisible = channel("cursor_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<ItemStack> cursorItem = channel("cursor_item", KeyframeFactories.ITEM_STACK);
   public final ValueStringKeys disabledTracks = new ValueStringKeys("pov_disabled_tracks");
   public final HudRecorder recording = new HudRecorder();

   public RecordedHudData() {
      for (int i = 0; i < 27; i++) {
         this.inventory.add(channel("inv_" + i, KeyframeFactories.ITEM_STACK));
      }
   }

   private static <T> KeyframeChannel<T> channel(String id, IKeyframeFactory<T> factory) {
      return new KeyframeChannel("hotbar_" + id, factory);
   }

   public void initializeReplayPreset(ReplayKeyframes replay) {
      this.insertBakedHudPreset(replay);
   }

   public void ensureNativeSlotDefaults(ReplayKeyframes replay) {
      for (KeyframeChannel<ItemStack> slot : replay.hotbar) {
         if (slot.isEmpty()) {
            slot.insert(0.0F, ItemStack.EMPTY);
         }
      }

      for (KeyframeChannel<ItemStack> slotx : this.inventory) {
         if (slotx.isEmpty()) {
            slotx.insert(0.0F, ItemStack.EMPTY);
         }
      }

      if (this.inventoryAnchor.isEmpty()) {
         this.inventoryAnchor.insert(0.0F, true);
      }

      if (replay.selectedSlot.isEmpty()) {
         replay.selectedSlot.insert(0.0F, 0);
      }

      if (PovSettings.isBakeActions()) {
         if (this.cursorVisible.isEmpty()) {
            this.cursorVisible.insert(0.0F, false);
         }

         if (this.cursorLayout.isEmpty()) {
            this.cursorLayout.insert(0.0F, new Transform());
         }

         if (this.cursorItem.isEmpty()) {
            this.cursorItem.insert(0.0F, ItemStack.EMPTY);
         }
      }
   }

   public void ensureStartRecordingBounds(float startTick, ReplayKeyframes targetReplay) {
      if (PovSettings.isBakeActions()) {
         float tick = Math.max(0.0F, startTick);
         Transform initialTransform = new Transform();
         if (targetReplay instanceof ReplayKeyframesPovAccess targetAccess) {
            RecordedHudData targetHud = targetAccess.bbsPov$getHud();
            if (targetHud != null && !targetHud.cursorLayout.isEmpty()) {
               initialTransform = ((Transform)targetHud.cursorLayout.interpolate(tick, new Transform())).copy();
            }
         }

         this.cursorVisible.insert(tick, false);
         PovActionClip.constant(this.cursorVisible);
         this.cursorLayout.insert(tick, initialTransform);
         this.cursorItem.insert(tick, ItemStack.EMPTY);
      }
   }

   public void ensureEndRecordingBounds(float endTick) {
      if (!(endTick <= 0.0F) && PovSettings.isBakeActions()) {
         if (!this.cursorVisible.isEmpty()) {
            List<? extends Keyframe<Boolean>> kfs = this.cursorVisible.getKeyframes();
            if (kfs.get(kfs.size() - 1).getTick() < endTick) {
               this.cursorVisible.insert(endTick, false);
            }
         } else {
            this.cursorVisible.insert(endTick, false);
         }

         PovActionClip.constant(this.cursorVisible);
         if (!this.cursorLayout.isEmpty()) {
            List<? extends Keyframe<Transform>> kfs = this.cursorLayout.getKeyframes();
            Keyframe<Transform> last = (Keyframe<Transform>)kfs.get(kfs.size() - 1);
            if (last.getTick() < endTick) {
               this.cursorLayout.insert(endTick, ((Transform)last.getValue()).copy());
            }
         } else {
            this.cursorLayout.insert(endTick, new Transform());
         }

         if (!this.cursorItem.isEmpty()) {
            List<? extends Keyframe<ItemStack>> kfs = this.cursorItem.getKeyframes();
            if (kfs.get(kfs.size() - 1).getTick() < endTick) {
               this.cursorItem.insert(endTick, ItemStack.EMPTY);
            }
         } else {
            this.cursorItem.insert(endTick, ItemStack.EMPTY);
         }
      }
   }

   public void trimCursorForRecordingRange(float startTick, float endTick) {
      trimChannelRange(this.cursorLayout, startTick, endTick);
      trimChannelRange(this.cursorVisible, startTick, endTick);
      trimChannelRange(this.cursorItem, startTick, endTick);
   }

   public void trimCursorForRecordingAt(float tick) {
      this.trimCursorForRecordingRange(tick, Float.MAX_VALUE);
   }

   private static <T> void trimChannelRange(KeyframeChannel<T> channel, float startTick, float endTick) {
      List<? extends Keyframe<T>> keyframes = channel.getKeyframes();

      for (int i = keyframes.size() - 1; i >= 0; i--) {
         float tick = keyframes.get(i).getTick();
         if (tick >= startTick - 1.0E-4F && tick <= endTick + 1.0E-4F) {
            channel.remove(i);
         }
      }
   }

   private void insertBakedHudPreset(ReplayKeyframes replay) {
      this.recording.reset();
      this.ensureNativeSlotDefaults(replay);

      for (KeyframeChannel<ItemStack> slot : this.inventory) {
         slot.removeAll();
         slot.insert(0.0F, ItemStack.EMPTY);
      }

      this.inventoryAnchor.removeAll();
      this.inventoryAnchor.insert(0.0F, true);
      reset(this.health, 20);
      reset(this.previousHealth, 20);
      reset(this.healthContainer, 20);
      reset(this.absorption, 0);
      reset(this.absorptionContainer, 0);
      reset(this.heartType, 0);
      reset(this.hardcore, false);
      reset(this.regeneration, false);
      reset(this.armor, 0);
      reset(this.hunger, 20);
      reset(this.hungerEffect, false);
      reset(this.mountHealth, 0);
      reset(this.mountHealthContainer, 0);
      reset(this.air, 300);
      reset(this.experience, 0.0);
      reset(this.experienceLevel, 0);
      reset(this.heartFlash, false);
      reset(this.absorptionFlash, false);
      reset(this.statusBarsVisible, true);
   }

   public void addTo(ValueGroup group) {
      group.add(this.disabledTracks);
      group.add(this.health);
      group.add(this.previousHealth);
      group.add(this.healthContainer);
      group.add(this.absorption);
      group.add(this.absorptionContainer);
      group.add(this.heartType);
      group.add(this.hardcore);
      group.add(this.regeneration);
      group.add(this.armor);
      group.add(this.hunger);
      group.add(this.hungerEffect);
      group.add(this.mountHealth);
      group.add(this.mountHealthContainer);
      group.add(this.air);
      group.add(this.experience);
      group.add(this.experienceLevel);
      group.add(this.heartFlash);
      group.add(this.attackCooldown);
      group.add(this.absorptionFlash);
      group.add(this.layout);
      group.add(this.visible);
      group.add(this.statusBarsVisible);
      group.add(this.crosshair);
      group.add(this.cursorLayout);
      group.add(this.cursorVisible);
      group.add(this.cursorItem);
      group.add(this.inventoryAnchor);

      for (KeyframeChannel<ItemStack> slot : this.inventory) {
         group.add(slot);
      }
   }

   public boolean hasRecordedData() {
      return !this.health.isEmpty() || !this.crosshair.isEmpty() || !this.cursorVisible.isEmpty() || !this.cursorLayout.isEmpty() || !this.cursorItem.isEmpty();
   }

   public int getDuration(ReplayKeyframes replay) {
      float last = Math.max(lastTick(replay.selectedSlot), lastTick(replay.offHand));

      for (KeyframeChannel<?> slot : replay.hotbar) {
         last = Math.max(last, lastTick(slot));
      }

      for (KeyframeChannel<?> slot : this.inventory) {
         last = Math.max(last, lastTick(slot));
      }

      for (KeyframeChannel<?> channel : this.getOwnedChannels()) {
         last = Math.max(last, lastTick(channel));
      }

      return Math.max(1, (int)Math.ceil((double)last) + 1);
   }

   public void addRecordingEndKeyframes(ReplayKeyframes replay, int endTick) {
      int tick = Math.max(0, endTick);
      addEndKeyframe(this.health, tick, 20);
      addEndKeyframe(this.previousHealth, tick, 20);
      addEndKeyframe(this.healthContainer, tick, 20);
      addEndKeyframe(this.absorption, tick, 0);
      addEndKeyframe(this.absorptionContainer, tick, 0);
      addEndKeyframe(this.heartType, tick, 0);
      addEndKeyframe(this.hardcore, tick, false);
      addEndKeyframe(this.regeneration, tick, false);
      addEndKeyframe(this.armor, tick, 0);
      addEndKeyframe(this.hunger, tick, 20);
      addEndKeyframe(this.hungerEffect, tick, false);
      addEndKeyframe(this.mountHealth, tick, 0);
      addEndKeyframe(this.mountHealthContainer, tick, 0);
      addEndKeyframe(this.air, tick, 300);
      addEndKeyframe(this.experience, tick, 0.0);
      addEndKeyframe(this.experienceLevel, tick, 0);
      addEndKeyframe(this.heartFlash, tick, false);
      addEndKeyframe(this.attackCooldown, tick, 1.0F);
      addEndKeyframe(this.absorptionFlash, tick, false);
      addEndKeyframe(this.statusBarsVisible, tick, true);
      addEndKeyframe(this.inventoryAnchor, tick, true);

      for (KeyframeChannel<ItemStack> slot : this.inventory) {
         addEndKeyframe(slot, tick, ItemStack.EMPTY);
      }
   }

   public void addNativeItemBoundaryKeyframes(ReplayKeyframes replay, int startTick, int endTick) {
      int start = Math.max(0, startTick);
      int end = Math.max(start, endTick);

      for (KeyframeChannel<ItemStack> slot : replay.hotbar) {
         addBoundaryKeyframes(slot, start, end);
      }

      for (KeyframeChannel<ItemStack> slot : this.inventory) {
         addBoundaryKeyframes(slot, start, end);
      }

      addBoundaryKeyframes(this.inventoryAnchor, start, end, true);
      addBoundaryKeyframes(replay.offHand, start, end);
      addBoundaryKeyframes(replay.selectedSlot, start, end, 0);
   }

   public KeyframeChannel<?>[] getOwnedChannels() {
      List<KeyframeChannel<?>> list = new ArrayList<>();
      list.add(this.layout);
      list.add(this.visible);
      list.add(this.statusBarsVisible);
      list.add(this.crosshair);
      list.add(this.health);
      list.add(this.previousHealth);
      list.add(this.healthContainer);
      list.add(this.absorption);
      list.add(this.absorptionContainer);
      list.add(this.heartType);
      list.add(this.hardcore);
      list.add(this.regeneration);
      list.add(this.armor);
      list.add(this.hunger);
      list.add(this.hungerEffect);
      list.add(this.mountHealth);
      list.add(this.mountHealthContainer);
      list.add(this.air);
      list.add(this.experience);
      list.add(this.experienceLevel);
      list.add(this.heartFlash);
      list.add(this.attackCooldown);
      list.add(this.absorptionFlash);
      list.add(this.inventoryAnchor);
      list.addAll(this.inventory);
      return list.toArray(new KeyframeChannel[0]);
   }

   private static ItemStack copyItem(ItemStack stack) {
      return stack == null ? ItemStack.EMPTY : stack.copy();
   }

   private static <T> void addEndKeyframe(KeyframeChannel<T> channel, int tick, T fallback) {
      T value = (T)channel.interpolate((float)tick, fallback);
      if (value instanceof ItemStack stack) {
         T copied = (T)stack.copy();
         value = copied;
      }

      channel.insert((float)tick, value);
   }

   private static void addBoundaryKeyframes(KeyframeChannel<ItemStack> channel, int start, int end) {
      ItemStack startValue = copyItem((ItemStack)channel.interpolate((float)start, ItemStack.EMPTY));
      channel.insert((float)start, startValue);
      ItemStack endValue = copyItem((ItemStack)channel.interpolate((float)end, startValue));
      channel.insert((float)end, endValue);
   }

   private static <T> void addBoundaryKeyframes(KeyframeChannel<T> channel, int start, int end, T fallback) {
      T startValue = (T)channel.interpolate((float)start, fallback);
      channel.insert((float)start, startValue);
      channel.insert((float)end, channel.interpolate((float)end, startValue));
   }

   private static <T> void reset(KeyframeChannel<T> channel, T value) {
      channel.insert(0.0F, value);
   }

   private static float lastTick(KeyframeChannel<?> channel) {
      List<? extends Keyframe<?>> keyframes = channel.getKeyframes();
      return keyframes.isEmpty() ? 0.0F : keyframes.get(keyframes.size() - 1).getTick();
   }
}
