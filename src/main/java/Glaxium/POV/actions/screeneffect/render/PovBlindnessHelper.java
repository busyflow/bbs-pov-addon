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
import net.minecraft.util.math.MathHelper;

public final class PovBlindnessHelper {
   public static final float DEFAULT_RADIUS = 5.0F;

   private PovBlindnessHelper() {
   }

   public static float resolveBlindnessFactor(float tickDelta) {
      PovBlindnessHelper.Sample sample = resolve(tickDelta);
      return sample != null ? sample.factor : -1.0F;
   }

   public static float resolveBlindnessRadius(float tickDelta) {
      PovBlindnessHelper.Sample sample = resolve(tickDelta);
      return sample != null ? sample.radius : 5.0F;
   }

   private static PovBlindnessHelper.Sample resolve(float tickDelta) {
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

                  float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                  return sampleFromReplay(replay, rTick);
               } else {
                  PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
                  if (clip != null) {
                     if (!(Boolean)clip.actions.get()) {
                        return null;
                     } else {
                        Replay replay = PovCameraClips.resolveReplay(film, clip);
                        float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                        return sampleFromReplay(replay, rTick);
                     }
                  } else if (povMode != 0 && povMode != 3) {
                     return null;
                  } else {
                     Replay replay = film.getFirstPersonReplay();
                     float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                     return sampleFromReplay(replay, rTick);
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

   private static PovBlindnessHelper.Sample sampleFromReplay(Replay replay, float replayTick) {
      if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions == null) {
            return null;
         } else {
            for (Clip clip : actions.get()) {
               if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("blindness")) {
                  float elapsed = se.getLocalTick(replayTick);
                  float opacity = se.blindnessOpacity.isEmpty() ? 1.0F : (Float)se.blindnessOpacity.interpolate(elapsed);
                  float radius = se.blindnessRadius.isEmpty() ? 5.0F : (Float)se.blindnessRadius.interpolate(elapsed);
                  return new PovBlindnessHelper.Sample(MathHelper.clamp(opacity, 0.0F, 1.0F), Math.max(0.5F, radius));
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static record Sample(float factor, float radius) {
   }
}
