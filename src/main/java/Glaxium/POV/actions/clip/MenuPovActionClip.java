package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import java.util.List;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

public final class MenuPovActionClip extends PovActionClip {
   public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
   public final KeyframeChannel<Transform> cursorLayout = this.channel("cursor_layout", KeyframeFactories.TRANSFORM);
   public final KeyframeChannel<Boolean> cursorVisible = this.channel("cursor_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<String> deathMessage = this.channel("death_message", KeyframeFactories.STRING);
   public final KeyframeChannel<String> score = this.channel("score", KeyframeFactories.STRING);
   public final KeyframeChannel<Float> bgOpacity = this.channel("bg_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> opacity = this.channel("opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Boolean> buttonsActive = this.channel("buttons_active", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> leaveBed = this.channel("leave_bed", KeyframeFactories.BOOLEAN);

   @Override
   public PovActionType getActionType() {
      return PovActionType.MENU;
   }

   public String resolveType() {
      return this.state.isEmpty() ? "game_menu" : (String)this.state.get(0).getValue();
   }

   @Override
   public void normalize() {
      super.normalize();
      clamp(this.bgOpacity, 0.0F, 1.0F);
      clamp(this.opacity, 0.0F, 1.0F);
   }

   public void ensureBakingBounds() {
      float end = (float)((Integer)this.duration.get()).intValue();
      padChannel(this.state, end);
      padChannel(this.cursorLayout, end);
      padChannel(this.cursorVisible, end);
      padChannel(this.deathMessage, end);
      padChannel(this.score, end);
      padChannel(this.bgOpacity, end, true);
      padChannel(this.opacity, end, false);
      padChannel(this.buttonsActive, end, true);
      padChannel(this.leaveBed, end, true);
      linear(this.opacity);
   }

   private static <T> void padChannel(KeyframeChannel<T> channel, float end) {
      padChannel(channel, end, true);
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
      return new MenuPovActionClip();
   }
}
