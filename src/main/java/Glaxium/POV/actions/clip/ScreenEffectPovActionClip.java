package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.screeneffect.ScreenEffectEntry;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ScreenEffectPovActionClip extends PovActionClip {
   public final ValueString activeEffects = new ValueString("active_effects", "");
   public final KeyframeChannel<Boolean> vignetteVisible = this.channel("vignette_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> vignetteOpacity = this.channel("vignette_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Color> vignetteColor = this.channel("vignette_color", KeyframeFactories.COLOR);
   public final KeyframeChannel<Boolean> fireVisible = this.channel("fire_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> frostVisible = this.channel("frost_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> frostProgress = this.channel("frost_progress", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> frostZoom = this.channel("frost_zoom", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> portalVisible = this.channel("portal_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> portalOpacity = this.channel("portal_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> pumpkinVisible = this.channel("pumpkin_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> pumpkinOpacity = this.channel("pumpkin_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> spyglassVisible = this.channel("spyglass_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> spyglassScale = this.channel("spyglass_scale", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> spyglassZoom = this.channel("spyglass_zoom", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> suffocationVisible = this.channel("suffocation_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<String> suffocationBlock = this.channel("suffocation_block", KeyframeFactories.STRING);
   public final KeyframeChannel<Float> suffocationOpacity = this.channel("suffocation_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> nightVisionVisible = this.channel("night_vision_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> nightVisionOpacity = this.channel("night_vision_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> nightVisionFlash = this.channel("night_vision_flash", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> blindnessVisible = this.channel("blindness_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> blindnessOpacity = this.channel("blindness_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> blindnessRadius = this.channel("blindness_radius", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> underwaterVisible = this.channel("underwater_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> underwaterOpacity = this.channel("underwater_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> totemVisible = this.channel("totem_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> totemProgress = this.channel("totem_progress", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> totemFlipped = this.channel("totem_flipped", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> totemParticles = this.channel("totem_particles", KeyframeFactories.BOOLEAN);
   public final ValueString totemItem = new ValueString("totem_item", "minecraft:totem_of_undying");
   public transient int lastTotemParticleTick = Integer.MIN_VALUE;
   public final KeyframeChannel<Boolean> nauseaVisible = this.channel("nausea_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> nauseaDistortion = this.channel("nausea_distortion", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> nauseaOpacity = this.channel("nausea_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> darknessVisible = this.channel("darkness_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> darknessOpacity = this.channel("darkness_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> darknessRadius = this.channel("darkness_radius", KeyframeFactories.FLOAT);
   public final ValueColor fireColor = new ValueColor("fire_color", new Color(1.0F, 1.0F, 1.0F, 1.0F));
   public final ValueColor portalColor = new ValueColor("portal_color", new Color(1.0F, 1.0F, 1.0F, 1.0F));
   public final ValueColor frostColor = new ValueColor("frost_color", new Color(1.0F, 1.0F, 1.0F, 1.0F));
   public final ValueColor underwaterColor = new ValueColor("underwater_color", new Color(1.0F, 1.0F, 1.0F, 1.0F));

   public ScreenEffectPovActionClip() {
      this.add(this.activeEffects);
      this.add(this.totemItem);
      this.add(this.fireColor);
      this.add(this.portalColor);
      this.add(this.frostColor);
      this.add(this.underwaterColor);
   }

   public Clip create() {
      ScreenEffectPovActionClip clip = new ScreenEffectPovActionClip();
      clip.copy(this);
      return clip;
   }

   @Override
   public PovActionType getActionType() {
      return PovActionType.SCREEN_EFFECT;
   }

   @Override
   public void normalize() {
      super.normalize();
      constant(this.vignetteVisible);
      clamp(this.vignetteOpacity, 0.0F, 1.0F);
      constant(this.fireVisible);
      constant(this.frostVisible);
      clamp(this.frostProgress, 0.0F, 1.0F);
      clamp(this.frostZoom, 0.1F, 2.0F);
      constant(this.portalVisible);
      clamp(this.portalOpacity, 0.0F, 1.0F);
      constant(this.pumpkinVisible);
      clamp(this.pumpkinOpacity, 0.0F, 1.0F);
      constant(this.spyglassVisible);
      clamp(this.spyglassScale, 0.1F, 10.0F);
      clamp(this.spyglassZoom, 0.001F, 1000.0F);
      constant(this.suffocationVisible);
      constant(this.suffocationBlock);
      clamp(this.suffocationOpacity, 0.0F, 1.0F);
      constant(this.nightVisionVisible);
      clamp(this.nightVisionOpacity, 0.0F, 1.0F);
      clamp(this.nightVisionFlash, 0.0F, 1.0F);
      constant(this.blindnessVisible);
      clamp(this.blindnessOpacity, 0.0F, 1.0F);
      clamp(this.blindnessRadius, 0.5F, 128.0F);
      constant(this.underwaterVisible);
      clamp(this.underwaterOpacity, 0.0F, 1.0F);
      constant(this.totemVisible);
      constant(this.totemFlipped);
      constant(this.totemParticles);
      clamp(this.totemProgress, 0.0F, 1.0F);
      constant(this.nauseaVisible);
      clamp(this.nauseaDistortion, 0.0F, 1.0F);
      clamp(this.nauseaOpacity, 0.0F, 1.0F);
      constant(this.darknessVisible);
      clamp(this.darknessOpacity, 0.0F, 1.0F);
      clamp(this.darknessRadius, 1.0F, 128.0F);
   }

   public List<String> getActiveEffectList() {
      String str = (String)this.activeEffects.get();
      if (str != null && !str.trim().isEmpty()) {
         List<String> list = new ArrayList<>();

         for (String part : str.split(",")) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
               list.add(trimmed);
            }
         }

         list.sort(Comparator.comparingInt(id -> ScreenEffectPresets.getById(id).vanillaRenderOrder));
         return list;
      } else {
         return new ArrayList<>();
      }
   }

   public boolean hasEffect(String id) {
      return this.getActiveEffectList().contains(id.toLowerCase());
   }

   public void addEffect(String id) {
      this.addEffect(id, true);
   }

   public void addEffect(String id, boolean seedDefaults) {
      if (id != null) {
         String lower = id.toLowerCase();
         List<String> list = this.getActiveEffectList();
         if (!list.contains(lower)) {
            list.add(lower);
            list.sort(Comparator.comparingInt(eid -> ScreenEffectPresets.getById(eid).vanillaRenderOrder));
            this.activeEffects.set(String.join(",", list));
            if (seedDefaults) {
               this.seedDefaultKeyframes(lower);
            }
         }
      }
   }

   public List<ScreenEffectEntry> getEffects() {
      List<ScreenEffectEntry> list = new ArrayList<>();

      for (String id : this.getActiveEffectList()) {
         list.add(new ScreenEffectEntry(id));
      }

      return list;
   }

   public void addEffect(ScreenEffectEntry entry) {
      if (entry != null) {
         this.addEffect(entry.getEffectId());
      }
   }

   public void removeEffect(ScreenEffectEntry entry) {
      if (entry != null) {
         this.removeEffect(entry.getEffectId());
      }
   }

   public void removeEffect(String id) {
      if (id != null) {
         String lower = id.toLowerCase();
         List<String> list = this.getActiveEffectList();
         if (list.remove(lower)) {
            this.activeEffects.set(String.join(",", list));
            this.clearEffectKeyframes(lower);
         }
      }
   }

   private static <T> void clearChannel(KeyframeChannel<T> channel) {
      while (!channel.isEmpty()) {
         channel.remove(0);
      }
   }

   public void clearEffectKeyframes(String id) {
      switch (id) {
         case "vignette":
            clearChannel(this.vignetteVisible);
            clearChannel(this.vignetteOpacity);
            clearChannel(this.vignetteColor);
            break;
         case "fire":
            clearChannel(this.fireVisible);
            break;
         case "frost":
            clearChannel(this.frostVisible);
            clearChannel(this.frostProgress);
            clearChannel(this.frostZoom);
            break;
         case "portal":
            clearChannel(this.portalVisible);
            clearChannel(this.portalOpacity);
            break;
         case "pumpkin":
            clearChannel(this.pumpkinVisible);
            clearChannel(this.pumpkinOpacity);
            break;
         case "spyglass":
            clearChannel(this.spyglassVisible);
            clearChannel(this.spyglassScale);
            clearChannel(this.spyglassZoom);
            break;
         case "suffocation":
            clearChannel(this.suffocationVisible);
            clearChannel(this.suffocationBlock);
            clearChannel(this.suffocationOpacity);
            break;
         case "night_vision":
            clearChannel(this.nightVisionVisible);
            clearChannel(this.nightVisionOpacity);
            clearChannel(this.nightVisionFlash);
            break;
         case "blindness":
            clearChannel(this.blindnessVisible);
            clearChannel(this.blindnessOpacity);
            clearChannel(this.blindnessRadius);
            break;
         case "totem":
            clearChannel(this.totemVisible);
            clearChannel(this.totemProgress);
            clearChannel(this.totemFlipped);
            clearChannel(this.totemParticles);
            break;
         case "nausea":
            clearChannel(this.nauseaVisible);
            clearChannel(this.nauseaDistortion);
            clearChannel(this.nauseaOpacity);
            break;
         case "darkness":
            clearChannel(this.darknessVisible);
            clearChannel(this.darknessOpacity);
            clearChannel(this.darknessRadius);
            break;
         case "underwater":
            clearChannel(this.underwaterVisible);
            clearChannel(this.underwaterOpacity);
      }
   }

   public void seedDefaultKeyframes(String id) {
      switch (id) {
         case "vignette":
            if (this.vignetteVisible.isEmpty()) {
               this.vignetteVisible.insert(0.0F, true);
            }

            if (this.vignetteOpacity.isEmpty()) {
               this.vignetteOpacity.insert(0.0F, 0.5F);
            }

            if (this.vignetteColor.isEmpty()) {
               this.vignetteColor.insert(0.0F, new Color(0.0F, 0.0F, 0.0F, 1.0F));
            }
            break;
         case "fire":
            if (this.fireVisible.isEmpty()) {
               this.fireVisible.insert(0.0F, true);
            }
            break;
         case "frost":
            if (this.frostVisible.isEmpty()) {
               this.frostVisible.insert(0.0F, true);
            }

            if (this.frostProgress.isEmpty()) {
               this.frostProgress.insert(0.0F, 1.0F);
            }

            if (this.frostZoom.isEmpty()) {
               this.frostZoom.insert(0.0F, 1.0F);
            }
            break;
         case "portal":
            if (this.portalVisible.isEmpty()) {
               this.portalVisible.insert(0.0F, true);
            }

            if (this.portalOpacity.isEmpty()) {
               this.portalOpacity.insert(0.0F, 1.0F);
            }
            break;
         case "pumpkin":
            if (this.pumpkinVisible.isEmpty()) {
               this.pumpkinVisible.insert(0.0F, true);
            }

            if (this.pumpkinOpacity.isEmpty()) {
               this.pumpkinOpacity.insert(0.0F, 1.0F);
            }
            break;
         case "spyglass":
            if (this.spyglassVisible.isEmpty()) {
               this.spyglassVisible.insert(0.0F, true);
            }

            if (this.spyglassScale.isEmpty()) {
               this.spyglassScale.insert(0.0F, 1.12F);
            }

            if (this.spyglassZoom.isEmpty()) {
               this.spyglassZoom.insert(0.0F, 0.102F);
            }
            break;
         case "suffocation":
            if (this.suffocationVisible.isEmpty()) {
               this.suffocationVisible.insert(0.0F, true);
            }

            if (this.suffocationBlock.isEmpty()) {
               this.suffocationBlock.insert(0.0F, "minecraft:stone");
            }
            break;
         case "night_vision":
            if (this.nightVisionVisible.isEmpty()) {
               this.nightVisionVisible.insert(0.0F, true);
            }

            if (this.nightVisionOpacity.isEmpty()) {
               this.nightVisionOpacity.insert(0.0F, 1.0F);
            }

            if (this.nightVisionFlash.isEmpty()) {
               this.nightVisionFlash.insert(0.0F, 0.0F);
            }
            break;
         case "blindness":
            if (this.blindnessVisible.isEmpty()) {
               this.blindnessVisible.insert(0.0F, true);
            }

            if (this.blindnessOpacity.isEmpty()) {
               this.blindnessOpacity.insert(0.0F, 1.0F);
            }

            if (this.blindnessRadius.isEmpty()) {
               this.blindnessRadius.insert(0.0F, 5.0F);
            }
            break;
         case "totem":
            if (this.totemVisible.isEmpty()) {
               this.totemVisible.insert(0.0F, true);
            }

            if (this.totemFlipped.isEmpty()) {
               this.totemFlipped.insert(0.0F, false);
            }

            if (this.totemParticles.isEmpty()) {
               this.totemParticles.insert(0.0F, true);
            }

            if (this.totemProgress.isEmpty()) {
               this.totemProgress.insert(0.0F, 0.0F);
               this.totemProgress.insert((float)Math.min(40, (Integer)this.duration.get()), 1.0F);
            }
            break;
         case "nausea":
            if (this.nauseaVisible.isEmpty()) {
               this.nauseaVisible.insert(0.0F, true);
            }

            if (this.nauseaDistortion.isEmpty()) {
               this.nauseaDistortion.insert(0.0F, 1.0F);
            }

            if (this.nauseaOpacity.isEmpty()) {
               this.nauseaOpacity.insert(0.0F, 1.0F);
            }
            break;
         case "darkness":
            if (this.darknessVisible.isEmpty()) {
               this.darknessVisible.insert(0.0F, true);
            }

            if (this.darknessOpacity.isEmpty()) {
               this.darknessOpacity.insert(0.0F, 1.0F);
            }

            if (this.darknessRadius.isEmpty()) {
               this.darknessRadius.insert(0.0F, 15.0F);
            }
            break;
         case "underwater":
            if (this.underwaterVisible.isEmpty()) {
               this.underwaterVisible.insert(0.0F, true);
            }

            if (this.underwaterOpacity.isEmpty()) {
               this.underwaterOpacity.insert(0.0F, 0.1F);
            }
      }
   }

   public void trimToRecording(int endTick) {
      int start = (Integer)this.tick.get();
      if (endTick > start && start + (Integer)this.duration.get() > endTick) {
         this.duration.set(Math.max(1, endTick - start));
      }
   }

   public void ensureBakingBounds() {
      this.padChannels((float)((Integer)this.duration.get()).intValue());
   }

   public void padChannelsToEnd(float end) {
      this.padChannels(end);
   }

   public void padChannels(float end) {
      padChannel(this.vignetteVisible, end, false);
      padChannel(this.vignetteOpacity, end, 0.0F);
      padChannel(this.vignetteColor, end, new Color(0.0F, 0.0F, 0.0F, 0.0F));
      padChannel(this.fireVisible, end, false);
      padChannel(this.frostVisible, end, false);
      padChannel(this.frostProgress, end, 0.0F);
      padChannel(this.frostZoom, end, 1.0F);
      padChannel(this.portalVisible, end, false);
      padChannel(this.portalOpacity, end, 0.0F);
      padChannel(this.nauseaVisible, end, false);
      padChannel(this.nauseaDistortion, end, 0.0F);
      padChannel(this.nauseaOpacity, end, 0.0F);
      padChannel(this.pumpkinVisible, end, false);
      padChannel(this.pumpkinOpacity, end, 0.0F);
      padChannel(this.spyglassVisible, end, false);
      padChannel(this.spyglassScale, end, 1.12F);
      padChannel(this.spyglassZoom, end, 1.0F);
      padChannel(this.suffocationVisible, end, false);
      padChannel(this.suffocationBlock, end, "minecraft:stone");
      padChannel(this.suffocationOpacity, end, 0.0F);
      padChannel(this.nightVisionVisible, end, false);
      padChannel(this.nightVisionOpacity, end, 0.0F);
      padChannel(this.nightVisionFlash, end, 0.0F);
      padChannel(this.blindnessVisible, end, false);
      padChannel(this.blindnessOpacity, end, 0.0F);
      padChannel(this.blindnessRadius, end, 5.0F);
      padChannel(this.totemVisible, end, false);
      padChannel(this.totemFlipped, end, false);
      padChannel(this.totemParticles, end, false);
      padChannel(this.totemProgress, end, 0.0F);
      padChannel(this.nauseaVisible, end, false);
      padChannel(this.nauseaDistortion, end, 0.0F);
      padChannel(this.nauseaOpacity, end, 0.0F);
      padChannel(this.darknessVisible, end, false);
      padChannel(this.darknessOpacity, end, 0.0F);
      padChannel(this.darknessRadius, end, 15.0F);
      padChannel(this.underwaterVisible, end, false);
      padChannel(this.underwaterOpacity, end, 0.0F);
   }

   private static <T> void padChannel(KeyframeChannel<T> channel, float end, T defaultStartValue) {
      if (channel != null && !channel.isEmpty() && !(end <= 0.0F)) {
         List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
         Keyframe<T> first = (Keyframe<T>)keyframes.get(0);
         Keyframe<T> last = (Keyframe<T>)keyframes.get(keyframes.size() - 1);
         if (first.getTick() > 0.0F) {
            channel.insert(0.0F, defaultStartValue);
            if (first.getTick() > 1.0F) {
               channel.insert(first.getTick() - 1.0F, defaultStartValue);
            }
         }

         if (last.getTick() < end) {
            channel.insert(end, last.getValue());
         }
      }
   }
}
