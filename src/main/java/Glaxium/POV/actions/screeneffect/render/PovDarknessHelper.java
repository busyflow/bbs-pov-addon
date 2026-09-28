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

public final class PovDarknessHelper {
   public static final float DEFAULT_RADIUS = 15.0F;

   private PovDarknessHelper() {
   }

   public static float resolveDarknessFactor(float tickDelta) {
      PovDarknessHelper.Sample sample = resolve(tickDelta);
      return sample != null ? sample.factor : -1.0F;
   }

   public static float resolveDarknessRadius(float tickDelta) {
      PovDarknessHelper.Sample sample = resolve(tickDelta);
      return sample != null ? sample.radius : 15.0F;
   }

   private static PovDarknessHelper.Sample resolve(float tickDelta) {
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

   private static PovDarknessHelper.Sample sampleFromReplay(Replay replay, float replayTick) {
      if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions == null) {
            return null;
         } else {
            for (Clip clip : actions.get()) {
               if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("darkness")) {
                  float elapsed = se.getLocalTick(replayTick);
                  float opacity = se.darknessOpacity.isEmpty() ? 1.0F : (Float)se.darknessOpacity.interpolate(elapsed);
                  float radius = se.darknessRadius.isEmpty() ? 15.0F : (Float)se.darknessRadius.interpolate(elapsed);
                  return new PovDarknessHelper.Sample(MathHelper.clamp(opacity, 0.0F, 1.0F), Math.max(1.0F, radius));
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
