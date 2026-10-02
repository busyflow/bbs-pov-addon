package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.controller.FilmEditorController;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

@Mixin(value = FilmEditorController.class, remap = false)
public class FilmEditorControllerPovMixin
{
    /**
     * In normal camera modes, Head Look makes the selected POV replay become
     * the camera itself. Do not render that same actor model into its own view.
     * This intentionally keys off the clip Replay Source, not Replay Editor
     * selection, so unrelated actors remain visible.
     */
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideHeadLookSourceActor(
        WorldRenderContext context,
        Replay replay,
        IEntity entity,
        CallbackInfo info)
    {
        FilmEditorController self = (FilmEditorController) (Object) this;
        UIFilmController controller = self.controller;

        if (Glaxium.POV.editor.UIPovHandEditor.isActive() && replay == controller.panel.replayEditor.getReplay())
        {
            info.cancel();
            return;
        }

        int povMode = controller.getPovMode();

        if (povMode == PovCameraMode.POV
            || povMode == UIFilmController.CAMERA_MODE_FREE
            || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            return;
        }

        Film film = self.film;
        float filmTick = controller.panel.getRunner() != null
            && controller.panel.getRunner().isRunning()
                ? controller.panel.getRunner().ticks + context.tickDelta()
                : controller.panel.getCursor();
        PovCameraClip clip = PovCameraClips.resolve(film, filmTick);

        if (clip != null
            && clip.headLook.get()
            && clip.isFirstPerson()
            && replay == PovCameraClips.resolveReplay(film, clip))
        {
            info.cancel();
        }
    }

    @Redirect(
        method = "renderEntity",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/film/controller/UIFilmController;getPovMode()I"))
    private int bbsPov$treatPovAsFirstPerson(UIFilmController controller)
    {
        int mode = controller.getPovMode();

        return mode == PovCameraMode.POV ? UIFilmController.CAMERA_MODE_FIRST_PERSON : mode;
    }
}
