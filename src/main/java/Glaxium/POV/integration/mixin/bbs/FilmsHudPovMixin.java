package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.RecorderPovAccess;
import Glaxium.POV.render.PovOverlayRenderer;
import Glaxium.POV.camera.clip.PovCameraClips;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Screen overlay pass belonging to BBS's actual Right-Control film playback. */
@Mixin(value = Films.class, remap = false)
public abstract class FilmsHudPovMixin
{
    @Shadow private Recorder recorder;
    @Shadow private java.util.List<mchorse.bbs_mod.film.BaseFilmController> controllers;
    @org.spongepowered.asm.mixin.Unique private final java.util.Map<mchorse.bbs_mod.film.BaseFilmController, Integer> bbsPov$lastPlaybackTicks = new java.util.HashMap<>();

    @Inject(method = "update", at = @At("HEAD"))
    private void bbsPov$dispatchThirdPersonPlaybackChat(CallbackInfo info)
    {
        if (this.controllers == null || this.controllers.isEmpty())
        {
            this.bbsPov$lastPlaybackTicks.clear();
            return;
        }

        if (mchorse.bbs_mod.BBSModClient.getVideoRecorder() != null && mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording())
        {
            return;
        }

        if (Glaxium.POV.playback.PovPlaybackContext.getActive() != null)
        {
            return;
        }

        for (mchorse.bbs_mod.film.BaseFilmController controller : this.controllers)
        {
            if (controller instanceof mchorse.bbs_mod.film.FirstPersonFilmController firstPerson)
            {
                Film film = firstPerson.film;
                if (film != null && !film.hasFirstPerson())
                {
                    int currentTick = firstPerson.getTick();
                    int lastTick = this.bbsPov$lastPlaybackTicks.getOrDefault(controller, -1);
                    if (lastTick < 0 || currentTick < lastTick || currentTick - lastTick > 100)
                    {
                        lastTick = currentTick - 1;
                    }
                    if (currentTick > lastTick)
                    {
                        int startRange = Math.max(0, lastTick + 1);
                        for (int t = startRange; t <= currentTick; t++)
                        {
                            Glaxium.POV.recording.PovRecordingSession.dispatchExternalChatMessages(film, -1, t);
                        }
                        this.bbsPov$lastPlaybackTicks.put(controller, currentTick);
                    }
                }
            }
        }
    }

    @Inject(method = "startRecording(Lmchorse/bbs_mod/film/Film;IIZ)V", at = @At("TAIL"))
    private void bbsPov$tagOutsideRecording(Film film, int replayId, int tick, boolean onMark, CallbackInfo info)
    {
        if (this.recorder instanceof RecorderPovAccess access)
        {
            access.bbsPov$setOutside(onMark);
        }
    }
    /** Keep world replay playback governed by the replay's playback toggle. */
    @Redirect(
        method = "playFilm(Lmchorse/bbs_mod/film/Film;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/film/Film;hasFirstPerson()Z"))
    private static boolean bbsPov$allowCameraTimelineForPovClips(Film film)
    {
        // Let BBS follow the replay whose First person playback toggle is on.
        // A migrated POV-only timeline must not seize the camera when every
        // replay has that toggle off. Keep ordinary camera films unchanged.
        return film.hasFirstPerson()
            || (PovCameraClips.hasAny(film)
                && film.camera.getClips(mchorse.bbs_mod.utils.clips.Clip.class).stream().allMatch(
                    clip -> clip instanceof Glaxium.POV.camera.clip.PovCameraClip));
    }

    @Inject(method = "renderHud", at = @At("RETURN"))
    private void bbsPov$renderPlaybackHud(Batcher2D batcher, float tickDelta, CallbackInfo info)
    {
        PovOverlayRenderer.renderPlayback(batcher, tickDelta);
    }
}
