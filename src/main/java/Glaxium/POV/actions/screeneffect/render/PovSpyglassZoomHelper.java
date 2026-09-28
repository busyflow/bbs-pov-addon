package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.utils.PovEffectSuppression;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.util.math.MathHelper;

public final class PovSpyglassZoomHelper {
   private PovSpyglassZoomHelper() {
   }

   public static float resolveZoomMultiplier(float tickDelta) {
      if (PovPlaybackContext.getActive(tickDelta) == null && !PovEffectSuppression.isBbsActive()) {
         return 1.0F;
      } else {
         PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
         if (playback != null) {
            return sampleFromReplay(playback.replay(), playback.replayTick());
         } else {
            UIFilmPanel panel = PovReplaySettings.getFilmPanel();
            if (panel != null && panel.hasParent() && panel.isVisible() && panel.getData() != null) {
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
                        if (clip.isFirstPerson() && (Boolean)clip.actions.get() && (Boolean)clip.screenEffects.get()) {
                           Replay replay = PovCameraClips.resolveReplay(film, clip);
                           float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                           return sampleFromReplay(replay, rTick);
                        } else {
                           return 1.0F;
                        }
                     } else if (povMode != 0 && povMode != 3) {
                        return 1.0F;
                     } else {
                        Replay replay = film.getFirstPersonReplay();
                        float rTick = replay != null ? (float)replay.getTick(cursor) + transition : filmTick;
                        return sampleFromReplay(replay, rTick);
                     }
                  }
               } else {
                  return 1.0F;
               }
            } else {
               return 1.0F;
            }
         }
      }
   }

   public static float sampleFromReplay(Replay replay, float replayTick) {
      if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedPovActions actions = access.bbsPov$getActions();
         if (actions == null) {
            return 1.0F;
         } else {
            float totalMultiplier = 1.0F;

            for (Clip clip : actions.get()) {
               if (clip instanceof ScreenEffectPovActionClip) {
                  ScreenEffectPovActionClip se = (ScreenEffectPovActionClip)clip;
                  if (se.isActive(replayTick)) {
                     float elapsed = se.getLocalTick(replayTick);
                     boolean spyglassActive = false;
                     if (se.hasEffect("spyglass") && !se.spyglassZoom.isEmpty()) {
                        float zoom = (Float)se.spyglassZoom.interpolate(elapsed);
                        if (zoom > 1.0E-4F && Math.abs(zoom - 1.0F) > 0.001F) {
                           totalMultiplier *= convertZoomToMultiplier(zoom);
                           spyglassActive = true;
                        }
                     }

                     if (!spyglassActive && se.hasEffect("frost") && !se.frostZoom.isEmpty()) {
                        float zoom = (Float)se.frostZoom.interpolate(elapsed);
                        if (zoom > 0.001F) {
                           totalMultiplier *= MathHelper.clamp(zoom, 0.1F, 2.0F);
                        }
                     }
                  }
               }
            }

            return totalMultiplier;
         }
      } else {
         return 1.0F;
      }
   }

   public static float convertZoomToMultiplier(float zoom) {
      if (zoom <= 1.0E-4F) {
         return 1.0F;
      } else {
         return zoom <= 1.0F ? MathHelper.clamp(zoom, 0.005F, 1.0F) : MathHelper.clamp(1.0F / zoom, 0.005F, 1.0F);
      }
   }
}
