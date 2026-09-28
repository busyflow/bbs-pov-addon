package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.RecorderPovAccess;
import Glaxium.POV.recording.PovRecordingSession;
import Glaxium.POV.render.PovOverlayRenderer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.Clip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Films.class},
   remap = false
)
public abstract class FilmsHudPovMixin {
   @Shadow
   private Recorder recorder;
   @Shadow
   private List<BaseFilmController> controllers;
   @Unique
   private final Map<BaseFilmController, Integer> bbsPov$lastPlaybackTicks = new HashMap<>();

   @Inject(
      method = {"update"},
      at = {@At("HEAD")}
   )
   private void bbsPov$dispatchThirdPersonPlaybackChat(CallbackInfo info) {
      if (this.controllers != null && !this.controllers.isEmpty()) {
         for (BaseFilmController controller : this.controllers) {
            if (controller instanceof FirstPersonFilmController) {
               FirstPersonFilmController firstPerson = (FirstPersonFilmController)controller;
               Film film = firstPerson.film;
               if (film != null && !film.hasFirstPerson()) {
                  int currentTick = firstPerson.getTick();
                  int lastTick = this.bbsPov$lastPlaybackTicks.getOrDefault(controller, -1);
                  if (currentTick > lastTick) {
                     int startRange = Math.max(0, lastTick + 1);

                     for (int t = startRange; t <= currentTick; t++) {
                        PovRecordingSession.dispatchExternalChatMessages(film, -1, t);
                     }

                     this.bbsPov$lastPlaybackTicks.put(controller, currentTick);
                  }
               }
            }
         }
      } else {
         this.bbsPov$lastPlaybackTicks.clear();
      }
   }

   @Inject(
      method = {"startRecording(Lmchorse/bbs_mod/film/Film;IIZ)V"},
      at = {@At("TAIL")}
   )
   private void bbsPov$tagOutsideRecording(Film film, int replayId, int tick, boolean onMark, CallbackInfo info) {
      if (this.recorder instanceof RecorderPovAccess access) {
         access.bbsPov$setOutside(onMark);
      }
   }

   @Redirect(
      method = {"playFilm(Lmchorse/bbs_mod/film/Film;Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/film/Film;shouldUseCameraTrack()Z"
      )
   )
   private static boolean bbsPov$allowCameraTimelineForPovClips(Film film) {
      return film.shouldUseCameraTrack() || (PovCameraClips.hasAny(film) && film.camera != null && film.camera.getClips(Clip.class).stream().allMatch(clip -> clip instanceof PovCameraClip));
   }

   @Inject(
      method = {"renderHud"},
      at = {@At("RETURN")}
   )
   private void bbsPov$renderPlaybackHud(Batcher2D batcher, float tickDelta, CallbackInfo info) {
      PovOverlayRenderer.renderPlayback(batcher, tickDelta);
   }
}
