package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the replay actor whose recorded head is currently driving playback camera. */
@Mixin(value = FirstPersonFilmController.class, remap = false)
public class FirstPersonFilmControllerPovMixin
{
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideHeadLookSourceActor(
        WorldRenderContext context,
        Replay replay,
        IEntity entity,
        CallbackInfo info)
    {
        FirstPersonFilmController controller = (FirstPersonFilmController) (Object) this;
        Film film = controller.film;
        if (replay != film.getFirstPersonReplay())
        {
            return;
        }
        float transition = controller.paused
            ? 0F
            : Math.max(0F, Math.min(1F, context.tickDelta()));
        float filmTick = controller.getTick() + transition;
        PovCameraClip clip = PovCameraClips.resolve(film, filmTick);

        if (clip != null
            && clip.headLook.get()
            && clip.isFirstPerson()
            && replay == PovCameraClips.resolveReplay(film, clip))
        {
            info.cancel();
        }
    }
}
