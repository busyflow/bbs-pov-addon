package Glaxium.POV.hand.playback;

import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.RecordedHandData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public final class HandSampler {
   private HandSampler() {
   }

   public static HandState sample(RecordedHandData data, ReplayKeyframes replay, float tick) {
      HandState state = new HandState();
      Pose basePose = data.getBasePose();
      Pose defaultPose = basePose != null ? basePose.copy() : new Pose();
      state.visible = (Boolean)data.visible.interpolate(tick, true);
      state.model = (String)data.model.interpolate(tick, null);
      state.texture = (Link)data.texture.interpolate(tick, null);
      state.color = data.color.isEmpty() ? null : (Color)data.color.interpolate(tick, null);
      state.colorOverlay = data.colorOverlay.isEmpty() ? null : (Color)data.colorOverlay.interpolate(tick, null);
      state.cameraOffset.copy((Transform)data.cameraOffset.interpolate(tick, new Transform()));
      state.pose.copy((Pose)data.pose.interpolate(tick, defaultPose));
      state.itemPose.copy((Pose)data.itemPose.interpolate(tick, defaultPose));
      state.rightHandVisible = (Boolean)data.rightHandVisible.interpolate(tick, true);
      state.leftHandVisible = (Boolean)data.leftHandVisible.interpolate(tick, false);
      PoseTransform defaultRight = basePose != null && basePose.get("right_arm") != null ? copyPoseTransform(basePose.get("right_arm")) : new PoseTransform();
      PoseTransform defaultLeft = basePose != null && basePose.get("left_arm") != null ? copyPoseTransform(basePose.get("left_arm")) : new PoseTransform();
      state.rightPose.copy((Transform)data.rightPose.interpolate(tick, defaultRight));
      state.leftPose.copy((Transform)data.leftPose.interpolate(tick, defaultLeft));
      state.mainHand = copyItem(replay.getMainHandStack(tick));
      state.offHand = copyItem((ItemStack)replay.offHand.interpolate(tick, ItemStack.EMPTY));
      state.rightSwingProgress = sampleSwingProgress(data.rightSwingProgress, tick);
      state.previousRightSwingProgress = state.rightSwingProgress;
      state.leftSwingProgress = sampleSwingProgress(data.leftSwingProgress, tick);
      state.previousLeftSwingProgress = state.leftSwingProgress;
      state.mainEquipProgress = clamp((Float)data.mainEquipProgress.interpolate(tick, 0.0F), 0.0F, 1.0F);
      state.previousMainEquipProgress = clamp((Float)data.mainEquipProgress.interpolate(tick - 1.0F, state.mainEquipProgress), 0.0F, 1.0F);
      state.offEquipProgress = clamp((Float)data.offEquipProgress.interpolate(tick, 0.0F), 0.0F, 1.0F);
      state.previousOffEquipProgress = clamp((Float)data.offEquipProgress.interpolate(tick - 1.0F, state.offEquipProgress), 0.0F, 1.0F);
      int wholeTick = (int)Math.floor((double)tick);
      int active = (Integer)data.activeHand.interpolate((float)wholeTick, 0);
      state.usingItem = active != 0;
      state.activeHand = active == 2 ? Hand.OFF_HAND : Hand.MAIN_HAND;
      state.activeItem = copyItem((ItemStack)data.activeItem.interpolate((float)wholeTick, ItemStack.EMPTY));
      state.showUseParticles = (Boolean)data.showUseParticles.interpolate(tick, true);
      state.useTime = sampleUseTime(data, tick, active);
      state.bobPhase = (Float)data.bobPhase.interpolate(tick, 0.0F);
      state.previousBobPhase = (Float)data.bobPhase.interpolate(tick - 1.0F, state.bobPhase);
      state.bobStrength = Math.max(0.0F, (Float)data.bobStrength.interpolate(tick, 0.0F));
      state.previousBobStrength = Math.max(0.0F, (Float)data.bobStrength.interpolate(tick - 1.0F, state.bobStrength));
      state.mainArm = data.mainArm.interpolate(tick, false) ? Arm.LEFT : Arm.RIGHT;
      state.viewYaw = ((Double)replay.yaw.interpolate(tick, 0.0)).floatValue();
      state.previousViewYaw = ((Double)replay.yaw.interpolate(tick - 1.0F, (double)state.viewYaw)).floatValue();
      state.viewPitch = ((Double)replay.pitch.interpolate(tick, 0.0)).floatValue();
      state.previousViewPitch = ((Double)replay.pitch.interpolate(tick - 1.0F, (double)state.viewPitch)).floatValue();
      state.renderYaw = (Float)data.renderYaw.interpolate(tick, state.viewYaw);
      state.previousRenderYaw = (Float)data.renderYaw.interpolate(tick - 1.0F, state.renderYaw);
      state.renderPitch = (Float)data.renderPitch.interpolate(tick, state.viewPitch);
      state.previousRenderPitch = (Float)data.renderPitch.interpolate(tick - 1.0F, state.renderPitch);
      state.worldInteraction = (Boolean)data.worldInteraction.interpolate(tick, false);
      state.replayInteraction = (Boolean)data.replayInteraction.interpolate(tick, false);
      return state;
   }

   private static float sampleSwingProgress(KeyframeChannel<Float> channel, float tick) {
      int wholeTick = (int)Math.floor((double)tick);
      float transition = tick - (float)wholeTick;
      float current = clamp((Float)channel.interpolate((float)wholeTick, 0.0F), 0.0F, 1.0F);
      if (transition <= 0.0F) {
         return current;
      } else {
         float next = clamp((Float)channel.interpolate((float)wholeTick + 1.0F, current), 0.0F, 1.0F);
         if (current > 0.0F && next < current) {
            float unwrapped = current + (next + 1.0F - current) * transition;
            return unwrapped >= 1.0F ? unwrapped - 1.0F : unwrapped;
         } else {
            return clamp((Float)channel.interpolate(tick, current), 0.0F, 1.0F);
         }
      }
   }

   private static float sampleUseTime(RecordedHandData data, float tick, int active) {
      if (active == 0) {
         return 0.0F;
      } else {
         int wholeTick = (int)Math.floor((double)tick);
         float transition = tick - (float)wholeTick;
         int current = Math.max(0, (Integer)data.useTime.interpolate((float)wholeTick, 0));
         if (transition <= 0.0F) {
            return (float)current;
         } else {
            int nextActive = (Integer)data.activeHand.interpolate((float)wholeTick + 1.0F, 0);
            int next = Math.max(0, (Integer)data.useTime.interpolate((float)wholeTick + 1.0F, current));
            if (nextActive != active) {
               return (float)current;
            } else if (next < current) {
               return (float)current + (float)(next - current) * transition;
            } else {
               return next - current > 2 ? (float)current : (float)current + (float)(next - current) * transition;
            }
         }
      }
   }

   private static ItemStack copyItem(ItemStack stack) {
      return stack == null ? ItemStack.EMPTY : stack.copy();
   }

   private static PoseTransform copyPoseTransform(Transform source) {
      PoseTransform copy = new PoseTransform();
      if (source != null) {
         copy.copy(source);
      }

      return copy;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
