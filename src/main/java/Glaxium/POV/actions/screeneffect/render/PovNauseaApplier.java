package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public final class PovNauseaApplier {
   private PovNauseaApplier() {
   }

   public static void apply(MatrixStack matrixStack, float tickDelta, int ticks) {
      PovNauseaApplier.Sample sample = resolve(tickDelta, ticks);
      if (sample != null && !(sample.intensity <= 0.001F)) {
         float nauseaIntensity = sample.intensity;
         float scale = 5.0F / (nauseaIntensity * nauseaIntensity + 5.0F) - nauseaIntensity * 0.04F;
         scale *= scale;
         float angle = sample.time * (float)sample.speed;
         RotationAxis axis = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
         matrixStack.multiply(axis.rotationDegrees(angle));
         matrixStack.scale(1.0F / scale, 1.0F, 1.0F);
         matrixStack.multiply(axis.rotationDegrees(-angle));
      }
   }

   private static PovNauseaApplier.Sample resolve(float tickDelta, int ticks) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
      if (playback != null) {
         return sampleFromReplay(playback.replay(), playback.replayTick(), playback.replayTick());
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

                  float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                  return sampleFromReplay(replay, rTick, filmTick);
               } else {
                  PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
                  if (clip != null) {
                     if (!(Boolean)clip.actions.get()) {
                        return null;
                     } else {
                        Replay replay = PovCameraClips.resolveReplay(film, clip);
                        float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                        return sampleFromReplay(replay, rTick, filmTick);
                     }
                  } else if (povMode != 0 && povMode != 3) {
                     return null;
                  } else {
                     Replay replay = film.getFirstPersonReplay();
                     float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                     return sampleFromReplay(replay, rTick, filmTick);
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

   private static PovNauseaApplier.Sample sampleFromReplay(Replay replay, float replayTick, float time) {
      if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions == null) {
            return null;
         } else {
            for (Clip clip : actions.get()) {
               if (clip instanceof ScreenEffectPovActionClip) {
                  ScreenEffectPovActionClip se = (ScreenEffectPovActionClip)clip;
                  if (se.isActive(replayTick)) {
                     float elapsed = se.getLocalTick(replayTick);
                     boolean hasNausea = se.hasEffect("nausea");
                     boolean hasPortal = se.hasEffect("portal");
                     if (hasNausea || hasPortal) {
                        float intensity = 0.0F;
                        if (hasNausea) {
                           float distortion = se.nauseaDistortion.isEmpty() ? 1.0F : (Float)se.nauseaDistortion.interpolate(elapsed);
                           float opacity = se.nauseaOpacity.isEmpty() ? 1.0F : (Float)se.nauseaOpacity.interpolate(elapsed);
                           intensity = Math.max(intensity, distortion * opacity);
                        }

                        if (hasPortal) {
                           float portalOp = se.portalOpacity.isEmpty() ? 1.0F : (Float)se.portalOpacity.interpolate(elapsed);
                           intensity = Math.max(intensity, portalOp);
                        }

                        if (intensity > 0.001F) {
                           int speed = hasPortal ? 20 : 7;
                           return new PovNauseaApplier.Sample(intensity, time, speed);
                        }
                     }
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static record Sample(float intensity, float time, int speed) {
   }
}
