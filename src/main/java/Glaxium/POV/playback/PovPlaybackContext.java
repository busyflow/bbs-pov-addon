package Glaxium.POV.playback;

import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.utils.VideoRecorder;

public final class PovPlaybackContext {
   private static FirstPersonFilmController exportController;
   private static int exportBaseFrame;
   private static float exportBaseCursor;
   private static boolean exportClockActive;

   private PovPlaybackContext() {
   }

   public static PovPlaybackContext.Frame getActive() {
      return getActive(0.0F);
   }

   public static PovPlaybackContext.Frame getActive(float tickDelta) {
      List<BaseFilmController> controllers = ((FilmsPovAccess)BBSModClient.getFilms()).bbsPov$getControllers();

      for (int i = controllers.size() - 1; i >= 0; i--) {
         BaseFilmController candidate = controllers.get(i);
         if (candidate instanceof FirstPersonFilmController) {
            FirstPersonFilmController firstPerson = (FirstPersonFilmController)candidate;
            Film film = firstPerson.film;
            Replay replay = film.getFirstPersonReplay();
            if (replay != null && !firstPerson.hasFinished()) {
               Recorder actorRecorder = BBSModClient.getFilms() != null ? BBSModClient.getFilms().getRecorder() : null;
               if (actorRecorder == null || actorRecorder.keyframes != replay.keyframes) {
                  int filmTick = firstPerson.getTick();
                  float transition = firstPerson.paused ? 0.0F : Math.max(0.0F, Math.min(1.0F, tickDelta));
                  float filmCursor = getFilmCursor(firstPerson, (float)filmTick + transition);
                  int wholeCursor = (int)Math.floor((double)filmCursor);
                  float cursorTransition = filmCursor - (float)wholeCursor;
                  PovCameraClip clip = PovCameraClips.resolve(film, filmCursor);
                  if (clip == null) {
                     clip = new PovCameraClip();
                     clip.selector.set(PovCameraClips.indexOfReplay(film, replay));
                  }

                  if (clip != null && replay != null) {
                     return new PovPlaybackContext.Frame(firstPerson, film, replay, clip, wholeCursor, (float)replay.getTick(wholeCursor) + cursorTransition);
                  }
               }
            }
         }
      }

      exportClockActive = false;
      exportController = null;
      return null;
   }

   private static float getFilmCursor(FirstPersonFilmController controller, float normalCursor) {
      VideoRecorder recorder = BBSModClient.getVideoRecorder();
      if (!recorder.isRecording()) {
         exportClockActive = false;
         exportController = null;
         return normalCursor;
      } else {
         int frame = recorder.getCounter();
         if (!exportClockActive || exportController != controller || frame < exportBaseFrame) {
            exportClockActive = true;
            exportController = controller;
            exportBaseFrame = frame;
            exportBaseCursor = normalCursor;
         }

         int frameRate = Math.max(1, BBSRendering.getVideoFrameRate());
         return exportBaseCursor + (float)(frame - exportBaseFrame) * (20.0F / (float)frameRate);
      }
   }

   public static record Frame(FirstPersonFilmController controller, Film film, Replay replay, PovCameraClip clip, int filmTick, float replayTick) {
   }
}
