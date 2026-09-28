package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.bossbar.BossBarTypeEntry;
import java.util.List;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class BossBarPovActionClip extends PovActionClip {
   public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
   public final KeyframeChannel<String> name = this.channel("name", KeyframeFactories.STRING);
   public final KeyframeChannel<Float> percent = this.channel("percent", KeyframeFactories.FLOAT);
   public final KeyframeChannel<String> color = this.channel("color", KeyframeFactories.STRING);
   public final KeyframeChannel<String> style = this.channel("style", KeyframeFactories.STRING);

   public BossBarPovActionClip() {
      this.ensureDefaults();
   }

   @Override
   public PovActionType getActionType() {
      return PovActionType.BOSS_BARS;
   }

   public String resolveType() {
      BossBarTypeEntry entry = BossBarTypeEntry.findById(this.state.isEmpty() ? "" : (String)this.state.get(0).getValue());
      return entry == null ? BossBarTypeEntry.DRAGON.id : entry.id;
   }

   public void applyTypeDefaults(BossBarTypeEntry type) {
      if (type == null) {
         type = BossBarTypeEntry.DRAGON;
      }

      setConstant(this.state, type.id);
      setConstant(this.name, type.defaultTitle);
      setConstant(this.color, type.defaultColor);
      setConstant(this.style, type.defaultStyle);
      if (this.percent.isEmpty()) {
         this.percent.insert(0.0F, 1.0F);
      }
   }

   public void ensureDefaults() {
      if (this.state.isEmpty()) {
         this.applyTypeDefaults(BossBarTypeEntry.DRAGON);
      }

      if (this.name.isEmpty()) {
         this.name.insert(0.0F, BossBarTypeEntry.DRAGON.defaultTitle);
      }

      if (this.percent.isEmpty()) {
         this.percent.insert(0.0F, 1.0F);
      }

      if (this.color.isEmpty()) {
         this.color.insert(0.0F, BossBarTypeEntry.DRAGON.defaultColor);
      }

      if (this.style.isEmpty()) {
         this.style.insert(0.0F, BossBarTypeEntry.DRAGON.defaultStyle);
      }
   }

   @Override
   public void normalize() {
      super.normalize();
      this.ensureDefaults();
      clamp(this.percent, 0.0F, 1.0F);

      for (Keyframe<String> keyframe : this.color.getKeyframes()) {
         keyframe.setValue(BossBarLooks.COLORS[BossBarLooks.colorIndex((String)keyframe.getValue())]);
      }

      for (Keyframe<String> keyframe : this.style.getKeyframes()) {
         keyframe.setValue(BossBarLooks.STYLES[BossBarLooks.styleIndex((String)keyframe.getValue())]);
      }
   }

   public void ensureBakingBounds() {
      float end = (float)((Integer)this.duration.get()).intValue();
      padChannel(this.state, end, true);
      padChannel(this.name, end, true);
      padChannel(this.percent, end, false);
      padChannel(this.color, end, true);
      padChannel(this.style, end, true);
      linear(this.percent);
      constant(this.state);
      constant(this.name);
      constant(this.color);
      constant(this.style);
   }

   private static <T> void setConstant(KeyframeChannel<T> channel, T value) {
      if (channel.isEmpty()) {
         channel.insert(0.0F, value);
      } else {
         channel.get(0).setValue(value);
      }
   }

   private static <T> void padChannel(KeyframeChannel<T> channel, float end, boolean hold) {
      if (channel != null && !channel.isEmpty() && !(end <= 0.0F)) {
         List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
         Keyframe<T> first = (Keyframe<T>)keyframes.get(0);
         Keyframe<T> last = (Keyframe<T>)keyframes.get(keyframes.size() - 1);
         if (first.getTick() > 0.0F) {
            channel.insert(0.0F, first.getValue());
         }

         if (last.getTick() < end) {
            channel.insert(end, last.getValue());
         }

         if (hold) {
            constant(channel);
         }
      }
   }

   protected Clip create() {
      return new BossBarPovActionClip();
   }
}
