package Glaxium.POV.hand.playback;

import Glaxium.POV.PovAddon;
import Glaxium.POV.actions.camera.CameraShakeApplier;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import java.util.Map.Entry;
import mchorse.bbs_mod.client.renderer.LivePlayerItemUse;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class PovHandPlayback {
   private static HandPlaybackSession active;
   private static boolean reportedFailure;
   public static boolean suppressFormTransform = false;

   private PovHandPlayback() {
   }

   public static boolean isSuppressFormTransform() {
      return suppressFormTransform || UIPovHandEditor.isActive() || isActive();
   }

   public static boolean isSuppressFormTransform(Form form) {
      return form != null && FormUtils.getRoot(form) == form ? isSuppressFormTransform() : false;
   }

   public static boolean isHandActive(float renderTransition) {
      MinecraftClient client = MinecraftClient.getInstance();
      ClientPlayerEntity player = client.player;
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
      UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;
      if (playback != null && !client.options.getPerspective().isFirstPerson()) {
         return false;
      } else if (player != null && (playback != null || panel != null)) {
         Replay replay;
         if (playback != null) {
            if (!(Boolean)playback.clip().hands.get()) {
               return false;
            }

            replay = playback.replay();
         } else {
            if (panel.getData() == null) {
               return false;
            }

            boolean inHandEditor = UIPovHandEditor.isActive();
            int povMode = panel.getController().getPovMode();
            if (!inHandEditor && (povMode == 1 || povMode == 2)) {
               return false;
            }

            boolean povEditMode = povMode == 6 || inHandEditor;
            if (povEditMode) {
               replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
               if (replay == null && inHandEditor && UIPovHandEditor.getActive() != null) {
                  replay = UIPovHandEditor.getActive().getReplay();
               }
            } else {
               Film film = (Film)panel.getData();
               int cursor = panel.getCursor();
               boolean isPlaying = !inHandEditor && panel.getController().isPlaying();
               float transition = isPlaying ? Math.max(0.0F, Math.min(1.0F, renderTransition)) : 0.0F;
               PovCameraClip clip = PovCameraClips.resolve(film, (float)cursor + transition);
               if (clip == null || !(Boolean)clip.hands.get()) {
                  return false;
               }

               replay = PovCameraClips.resolveReplay(film, clip);
            }
         }

         return replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess;
      } else {
         return false;
      }
   }

   public static HandState resolveActiveState(float renderTransition) {
      if (active != null) {
         return active.state;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
         UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;
         if (playback != null && !client.options.getPerspective().isFirstPerson()) {
            return null;
         } else if (playback == null && panel == null) {
            return null;
         } else {
            Replay replay;
            float tick;
            if (playback != null) {
               if (!(Boolean)playback.clip().hands.get()) {
                  return null;
               }

               replay = playback.replay();
               tick = playback.replayTick();
            } else {
               if (panel.getData() == null) {
                  return null;
               }

               int povMode = panel.getController().getPovMode();
               boolean inHandEditor = UIPovHandEditor.isActive();
               if (!inHandEditor && (povMode == 1 || povMode == 2)) {
                  return null;
               }

               boolean povEditMode = povMode == 6 || inHandEditor;
               Film film = (Film)panel.getData();
               int cursor = panel.getCursor();
               boolean isPlaying = !inHandEditor && panel.getController().isPlaying();
               float transition = isPlaying ? Math.max(0.0F, Math.min(1.0F, renderTransition)) : 0.0F;
               if (povEditMode) {
                  replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
                  if (replay == null && inHandEditor && UIPovHandEditor.getActive() != null) {
                     replay = UIPovHandEditor.getActive().getReplay();
                  }
               } else {
                  PovCameraClip clip = PovCameraClips.resolve(film, (float)cursor + transition);
                  if (clip == null || !(Boolean)clip.hands.get()) {
                     return null;
                  }

                  replay = PovCameraClips.resolveReplay(film, clip);
               }

               if (replay == null) {
                  return null;
               }

               tick = (float)replay.getTick(cursor) + transition;
            }

            if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
               RecordedHandData data = access.bbsPov$getHand();
               if (data == null) {
                  return null;
               }

               return HandSampler.sample(data, replay.keyframes, tick);
            }

            return null;
         }
      }
   }

   public static boolean begin(float renderTransition) {
      if (active != null) {
         end();
      }

      MinecraftClient client = MinecraftClient.getInstance();
      ClientPlayerEntity player = client.player;
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(renderTransition);
      UIFilmPanel panel = playback == null ? PovReplaySettings.getFilmPanel() : null;
      if (playback != null && !client.options.getPerspective().isFirstPerson()) {
         return false;
      } else if (player != null && (playback != null || panel != null)) {
         Replay replay;
         float tick;
         boolean isPlaying;
         float transition;
         if (playback != null) {
            if (!(Boolean)playback.clip().hands.get()) {
               return false;
            }

            replay = playback.replay();
            tick = playback.replayTick();
            isPlaying = !playback.controller().paused;
            transition = isPlaying ? Math.max(0.0F, Math.min(1.0F, renderTransition)) : 0.0F;
         } else {
            boolean inHandEditor = UIPovHandEditor.isActive();
            int povMode = panel.getController().getPovMode();
            if (!inHandEditor && (povMode == 1 || povMode == 2)) {
               return false;
            }

            boolean povEditMode = povMode == 6 || inHandEditor;
            Film film = (Film)panel.getData();
            int cursor = panel.getCursor();
            isPlaying = !inHandEditor && panel.getController().isPlaying();
            transition = isPlaying ? Math.max(0.0F, Math.min(1.0F, renderTransition)) : 0.0F;
            if (povEditMode) {
               replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
               if (replay == null && inHandEditor && UIPovHandEditor.getActive() != null) {
                  replay = UIPovHandEditor.getActive().getReplay();
               }
            } else {
               PovCameraClip clip = PovCameraClips.resolve(film, (float)cursor + transition);
               if (clip == null || !(Boolean)clip.hands.get()) {
                  return false;
               }

               replay = PovCameraClips.resolveReplay(film, clip);
            }

            if (replay == null) {
               return false;
            }

            tick = (float)replay.getTick(cursor) + transition;
         }

         if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedHandData var18 = access.bbsPov$getHand();
            HandState state;
            if (UIPovHandEditor.isActive()) {
               state = HandStateApplier.createDefaultHandEditorState(var18, replay);
            } else {
               state = var18 == null ? null : HandSampler.sample(var18, replay.keyframes, tick);
               if (state == null) {
                  return false;
               }

               HandStateApplier.inheritReplayModel(var18, state, replay, panel);
            }

            if (playback != null) {
               LivePlayerItemUse.endFrame();
            }

            HandPlaybackSession next = new HandPlaybackSession(player, client.gameRenderer.firstPersonRenderer, state, var18, tick, isPlaying, transition);

            try {
               next.apply(replay);
               active = next;
               if (!PovHandPicking.isStencilPass()) {
                  PovHandMatrices.clear();
                  PovHandPicking.clearItemBounds();
               }

               reportedFailure = false;
               return true;
            } catch (Throwable var15) {
               next.restore();
               if (!reportedFailure) {
                  PovAddon.LOGGER.error("Couldn't prepare BBS POV hand playback", var15);
                  reportedFailure = true;
               }

               return false;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean render(HeldItemRenderer renderer, float tickDelta, MatrixStack matrices, Immediate consumers, ClientPlayerEntity player, int light) {
      HandPlaybackSession current = active;
      if (current == null || current.player != player) {
         return false;
      } else if (!current.state.visible) {
         consumers.draw();
         end();
         return true;
      } else {
         matrices.push();

         try {
            CameraShakeApplier.apply(matrices, tickDelta);
            MatrixStackUtils.applyTransform(matrices, current.state.cameraOffset);
            renderer.renderItem(current.getItemRenderTickDelta(), matrices, consumers, player, light);
            if (PovHandPicking.isStencilPass()) {
               current.renderBodyParts(light, PovHandPicking.getStencilMap());
            } else {
               current.renderBodyParts(light);
            }
         } finally {
            matrices.pop();
            end();
         }

         return true;
      }
   }

   public static void applyBob(MatrixStack matrices, float influence) {
      HandPlaybackSession current = active;
      if (current != null && current.state != null && influence > 0.0F) {
         applyBob(matrices, current.state, influence);
      }
   }

   public static void applyBob(MatrixStack matrices, HandState state, float influence) {
      if (state != null && !(influence <= 0.0F)) {
         float phase = -state.bobPhase;
         float strength = state.bobStrength * influence;
         float sin = MathHelper.sin(phase * (float) Math.PI);
         float cos = MathHelper.cos(phase * (float) Math.PI);
         matrices.translate(sin * strength * 0.5F, -Math.abs(cos * strength), 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin * strength * 3.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(phase * (float) Math.PI - 0.2F) * strength) * 5.0F));
      }
   }

   public static void applyTransforms(MatrixStack matrices) {
      HandPlaybackSession current = active;
      if (current != null) {
         matrices.push();
         CameraShakeApplier.apply(matrices, MinecraftClient.getInstance().getTickDelta());
         MatrixStackUtils.applyTransform(matrices, current.state.cameraOffset);
      }
   }

   public static void popTransforms(MatrixStack matrices) {
      matrices.pop();
   }

   public static float getCurrentItemRenderTickDelta(float fallback) {
      HandPlaybackSession current = active;
      return current == null ? fallback : current.getItemRenderTickDelta();
   }

   public static void end() {
      HandPlaybackSession current = active;
      active = null;
      if (current != null) {
         current.restore();
      }
   }

   public static boolean isActive() {
      return active != null;
   }

   public static HandState getActiveState() {
      return active != null ? active.state : null;
   }

   public static boolean shouldRenderModelHand(Hand hand) {
      HandPlaybackSession current = active;
      return current == null || (hand == Hand.MAIN_HAND ? current.state.rightHandVisible : current.state.leftHandVisible);
   }

   public static boolean shouldRenderArm(Arm arm) {
      HandPlaybackSession current = active;
      return current == null || (arm == Arm.RIGHT ? current.state.rightHandVisible : current.state.leftHandVisible);
   }

   public static Pose getRenderPose() {
      HandPlaybackSession current = active;
      return current == null ? null : current.renderPose;
   }

   public static PoseTransform getItemPose(boolean leftHanded) {
      HandPlaybackSession current = active;
      if (current == null) {
         return new PoseTransform();
      } else {
         String bone = PovItemPose.bone(leftHanded, current.state.mainArm);
         return PovItemPose.transform(current.state.itemPose, bone);
      }
   }

   public static String getItemBone(boolean leftHanded) {
      HandPlaybackSession current = active;
      return current == null ? null : PovItemPose.bone(leftHanded, current.state.mainArm);
   }

   public static void useCapturedPose(Pose pose) {
      HandPlaybackSession current = active;
      if (current != null && pose != null) {
         current.renderPose = pose.copy();
      }
   }

   public static void renderBodyPartsForPicking(int light, StencilMap stencilMap) {
      HandPlaybackSession current = active;
      if (current != null) {
         current.renderBodyParts(light, stencilMap);
      }
   }

   public static void renderBodyParts(int light) {
      HandPlaybackSession current = active;
      if (current != null) {
         current.renderBodyParts(light);
      }
   }

   public static float getActiveTransition() {
      HandPlaybackSession current = active;
      return current != null ? current.transition : 0.0F;
   }

   public static float getArmFix(Arm arm) {
      HandPlaybackSession current = active;
      if (current == null) {
         return 0.0F;
      } else {
         float maxFix = 0.0F;
         String bone = arm == Arm.RIGHT ? "right_arm" : "left_arm";
         PoseTransform pose = current.state != null ? (arm == Arm.RIGHT ? current.state.rightPose : current.state.leftPose) : null;
         if (pose != null && pose.fix > 0.0F) {
            maxFix = Math.max(maxFix, pose.fix);
         }

         if (current.state != null && current.state.pose != null) {
            maxFix = Math.max(maxFix, getPoseFixForArm(current.state.pose, bone));
         }

         if (current.povForm != null) {
            if (current.povForm.pose.get() != null) {
               maxFix = Math.max(maxFix, getPoseFixForArm((Pose)current.povForm.pose.get(), bone));
            }

            if (current.povForm.poseOverlay.get() != null) {
               maxFix = Math.max(maxFix, getPoseFixForArm((Pose)current.povForm.poseOverlay.get(), bone));
            }

            for (ValuePose addOverlay : current.povForm.additionalOverlays) {
               if (addOverlay.get() != null) {
                  maxFix = Math.max(maxFix, getPoseFixForArm((Pose)addOverlay.get(), bone));
               }
            }
         }

         return MathHelper.clamp(maxFix, 0.0F, 1.0F);
      }
   }

   private static float getPoseFixForArm(Pose pose, String armRoot) {
      if (pose != null && !pose.transforms.isEmpty()) {
         float max = 0.0F;

         for (Entry<String, PoseTransform> entry : pose.transforms.entrySet()) {
            String name = entry.getKey();
            PoseTransform pt = entry.getValue();
            if (pt != null && pt.fix > 0.0F && (name.equals(armRoot) || name.startsWith(armRoot) || name.contains(armRoot))) {
               max = Math.max(max, pt.fix);
            }
         }

         return max;
      } else {
         return 0.0F;
      }
   }

   public static float getHandFix(Hand hand) {
      HandPlaybackSession current = active;
      if (current != null && current.player != null) {
         Arm arm = hand == Hand.MAIN_HAND ? current.player.getMainArm() : current.player.getMainArm().getOpposite();
         return getArmFix(arm);
      } else {
         return 0.0F;
      }
   }

   public static void applyHandAnimations(MatrixStack matrices, Hand hand) {
      HandPlaybackSession current = active;
      if (current != null && current.state != null) {
         float fix = getHandFix(hand);
         float influence = 1.0F - fix;
         if (!(influence <= 0.0F)) {
            float pitchDiff = current.state.viewPitch - current.state.renderPitch;
            float yawDiff = current.state.viewYaw - current.state.renderYaw;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDiff * 0.1F * influence));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDiff * 0.1F * influence));
            applyBob(matrices, current.state, influence);
         }
      }
   }
}
