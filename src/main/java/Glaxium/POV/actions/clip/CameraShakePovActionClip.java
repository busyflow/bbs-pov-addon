package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import java.util.List;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class CameraShakePovActionClip extends PovActionClip {
   public final KeyframeChannel<Boolean> active = this.channel("active", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> hurtTime = this.channel("hurt_time", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> maxHurtTime = this.channel("max_hurt_time", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Float> damageTiltYaw = this.channel("damage_tilt_yaw", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Integer> deathTime = this.channel("death_time", KeyframeFactories.INTEGER);

   @Override
   public PovActionType getActionType() {
      return PovActionType.CAMERA_SHAKE;
   }

   @Override
   public void normalize() {
      super.normalize();
      clamp(this.hurtTime, 0, 200);
      clamp(this.maxHurtTime, 0, 200);
      clamp(this.deathTime, 0, 200);
   }

   public void ensureBakingBounds() {
      float end = (float)((Integer)this.duration.get()).intValue();
      padChannel(this.active, end);
      padChannel(this.hurtTime, end);
      padChannel(this.maxHurtTime, end);
      padChannel(this.damageTiltYaw, end);
      padChannel(this.deathTime, end);
   }

   private static <T> void padChannel(KeyframeChannel<T> channel, float end) {
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

         constant(channel);
      }
   }

   protected Clip create() {
      return new CameraShakePovActionClip();
   }
}
