package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.controller.FilmEditorController;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {FilmEditorController.class},
   remap = false
)
public class FilmEditorControllerPovMixin {
   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$hideHeadLookSourceActor(WorldRenderContext context, Replay replay, IEntity entity, CallbackInfo info) {
      FilmEditorController self = (FilmEditorController)this;
      UIFilmController controller = self.controller;
      if (UIPovHandEditor.isActive() && replay == controller.panel.replayEditor.getReplay()) {
         info.cancel();
      } else {
         int povMode = controller.getPovMode();
         if (povMode != 6 && povMode != 1 && povMode != 2) {
            Film film = self.film;
            float filmTick = controller.panel.getRunner() != null && controller.panel.getRunner().isRunning()
               ? (float)controller.panel.getRunner().ticks + context.tickDelta()
               : (float)controller.panel.getCursor();
            PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
            if (clip != null && (Boolean)clip.headLook.get() && clip.isFirstPerson() && replay == PovCameraClips.resolveReplay(film, clip)) {
               info.cancel();
            }
         }
      }
   }

   @Redirect(
      method = {"renderEntity"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/ui/film/controller/UIFilmController;getPovMode()I"
      )
   )
   private int bbsPov$treatPovAsFirstPerson(UIFilmController controller) {
      int mode = controller.getPovMode();
      return mode == 6 ? 3 : mode;
   }
}
