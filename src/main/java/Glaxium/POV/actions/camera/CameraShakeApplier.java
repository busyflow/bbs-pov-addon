package Glaxium.POV.actions.camera;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class CameraShakeApplier {
   private static CameraShakeApplier.Sample cachedSample;
   private static float cachedTickDelta = Float.NaN;

   private CameraShakeApplier() {
   }

   public static boolean shouldApply(float tickDelta) {
      return resolve(tickDelta) != null;
   }

   public static boolean shouldCancelVanilla(float tickDelta) {
      cachedSample = resolve(tickDelta);
      cachedTickDelta = tickDelta;
      return PovPlaybackContext.getActive(tickDelta) != null || cachedSample != null;
   }

   public static void apply(MatrixStack matrices, float tickDelta) {
      CameraShakeApplier.Sample sample = tickDelta == cachedTickDelta ? cachedSample : resolve(tickDelta);
      cachedSample = null;
      cachedTickDelta = Float.NaN;
      if (sample != null) {
         applySample(matrices, sample, frozenDelta(tickDelta));
      }
   }

   private static float frozenDelta(float tickDelta) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
      if (playback != null) {
         return playback.controller().paused ? 0.0F : tickDelta;
      } else {
         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         return panel != null && !panel.getController().isPlaying() ? 0.0F : tickDelta;
      }
   }

   private static CameraShakeApplier.Sample resolve(float tickDelta) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
      if (playback != null) {
         return sampleFromReplay(playback.replay(), playback.replayTick());
      } else {
         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         if (panel != null && panel.getData() != null) {
            int povMode = panel.getController().getPovMode();
            if (povMode != 1 && povMode != 2) {
               Film film = (Film)panel.getData();
               int cursor = panel.getCursor();
               boolean playing = panel.getController().isPlaying();
               float transition = playing ? Math.max(0.0F, Math.min(1.0F, tickDelta)) : 0.0F;
               float filmTick = (float)cursor + transition;
               boolean povEditMode = povMode == 6;
               if (povEditMode) {
                  Replay replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
                  if (replay == null) {
                     replay = film.getFirstPersonReplay();
                  }

                  return !PovReplaySettings.isCameraShakeEnabled(replay) ? null : sampleFromReplay(replay, replayTick(replay, cursor, transition));
               } else {
                  PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
                  if (clip != null) {
                     if (!(Boolean)clip.cameraShake.get()) {
                        return null;
                     } else {
                        Replay replay = PovCameraClips.resolveReplay(film, clip);
                        return sampleFromReplay(replay, replayTick(replay, cursor, transition));
                     }
                  } else if (povMode != 0 && povMode != 3) {
                     return null;
                  } else {
                     Replay replay = film.getFirstPersonReplay();
                     return sampleFromReplay(replay, replayTick(replay, cursor, transition));
                  }
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private static float replayTick(Replay replay, int cursor, float transition) {
      return replay == null ? (float)cursor + transition : (float)replay.getTick(cursor) + transition;
   }

   private static CameraShakeApplier.Sample sampleFromReplay(Replay replay, float replayTick) {
      if (replay != null && PovReplaySettings.isCameraShakeEnabled(replay) && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions == null) {
            return null;
         } else {
            CameraShakePovActionClip shake = actions.getActiveCameraShake(replayTick);
            if (shake == null) {
               return null;
            } else {
               float local = shake.getLocalTick(replayTick);
               boolean active = Boolean.TRUE.equals(shake.active.interpolate(local, false));
               if (!active) {
                  return null;
               } else {
                  int hurtTime = (Integer)shake.hurtTime.interpolate(local, 0);
                  int maxHurtTime = Math.max(1, (Integer)shake.maxHurtTime.interpolate(local, 10));
                  float yaw = (Float)shake.damageTiltYaw.interpolate(local, 0.0F);
                  int deathTime = (Integer)shake.deathTime.interpolate(local, 0);
                  return new CameraShakeApplier.Sample(hurtTime, maxHurtTime, yaw, deathTime);
               }
            }
         }
      } else {
         return null;
      }
   }

   private static void applySample(MatrixStack matrices, CameraShakeApplier.Sample sample, float tickDelta) {
      if (sample.deathTime > 0) {
         float death = Math.min((float)sample.deathTime + tickDelta, 20.0F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(40.0F - 8000.0F / (death + 200.0F)));
      }

      float remaining = (float)sample.hurtTime - tickDelta;
      if (!(remaining < 0.0F)) {
         float t = remaining / (float)sample.maxHurtTime;
         t = MathHelper.sin(t * t * t * t * (float) Math.PI);
         float strength = 1.0F;
         SimpleOption<Double> option = MinecraftClient.getInstance().options.getDamageTiltStrength();
         if (option != null) {
            strength = ((Double)option.getValue()).floatValue();
         }

         float roll = -t * 14.0F * strength;
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-sample.damageTiltYaw));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sample.damageTiltYaw));
      }
   }

   private static record Sample(int hurtTime, int maxHurtTime, float damageTiltYaw, int deathTime) {
   }
}
