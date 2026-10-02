package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.RecorderPovAccess;
import Glaxium.POV.recording.PovRecordingSession;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.forms.forms.Form;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Recorder.class, remap = false)
public class RecorderPovMixin implements RecorderPovAccess
{
    @Unique
    private PovRecordingSession bbsPov$session;

    @Unique
    private boolean bbsPov$outside;

    @Override
    public boolean bbsPov$isOutside()
    {
        return this.bbsPov$outside;
    }

    @Override
    public void bbsPov$setOutside(boolean outside)
    {
        this.bbsPov$outside = outside;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsPov$startSession(Film film, Form form, int replayIndex, int tick, CallbackInfo info)
    {
        this.bbsPov$session = PovRecordingSession.start((Recorder) (Object) this, film, form, replayIndex, tick);
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void bbsPov$recordFrame(CallbackInfo info)
    {
        if (this.bbsPov$session != null)
        {
            this.bbsPov$session.recordFrame((Recorder) (Object) this);
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void bbsPov$finishSession(CallbackInfo info)
    {
        if (this.bbsPov$session != null)
        {
            this.bbsPov$session.finish((Recorder) (Object) this);
        }
    }
}
