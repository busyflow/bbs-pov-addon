package Glaxium.POV.playback;

import Glaxium.POV.integration.access.bbs.BBSModClientAccess;
import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import java.util.List;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Films;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public final class PovPlaybackInput {
   private PovPlaybackInput() {
   }

   public static boolean isWorldPlaybackRunning() {
      Films films = BBSModClient.getFilms();
      if (films == null) {
         return false;
      } else {
         List<BaseFilmController> controllers = ((FilmsPovAccess)films).bbsPov$getControllers();
         if (controllers != null) {
            for (BaseFilmController controller : controllers) {
               if (!controller.hasFinished()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static boolean isLocked() {
      Films films = BBSModClient.getFilms();
      return films != null && films.getRecorder() == null ? PovPlaybackContext.getActive() != null : false;
   }

   public static boolean isAllowed(int key, int scancode) {
      try {
         KeyBinding playFilm = BBSModClientAccess.bbsPov$getKeyPlayFilm();
         if (playFilm != null && playFilm.matchesKey(key, scancode)) {
            return true;
         }

         KeyBinding recordVideo = BBSModClientAccess.bbsPov$getKeyRecordVideo();
         if (recordVideo != null && recordVideo.matchesKey(key, scancode)) {
            return true;
         }

         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.options != null && mc.options.togglePerspectiveKey != null && mc.options.togglePerspectiveKey.matchesKey(key, scancode)) {
            return true;
         }
      } catch (Throwable var5) {
      }

      return key == 345 || key == 293 || key == 294;
   }
}
