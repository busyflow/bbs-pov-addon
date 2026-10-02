package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.playback.PovPlaybackInput;
import mchorse.bbs_mod.BBSModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BBSModClient.class, remap = false)
public abstract class BBSModClientPovMixin
{
    @Inject(method = "keyRecordReplay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$blockRecordReplayDuringPlayback(CallbackInfo info)
    {
        if (PovPlaybackInput.isLocked())
        {
            info.cancel();
        }
    }

    @Inject(method = {"keyPlayFilm", "keyPlayFilmAndRecord"}, at = @At("HEAD"), cancellable = true)
    private void bbsPov$blockPlayFilmDuringRecording(CallbackInfo info)
    {
        if (BBSModClient.getFilms() != null && BBSModClient.getFilms().getRecorder() != null)
        {
            info.cancel();
        }
    }
}
