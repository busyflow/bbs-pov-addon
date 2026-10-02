package wemppy.bbs_pov.client.mixin;

import mchorse.bbs_mod.camera.clips.overwrite.POVClip;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import wemppy.bbs_pov.client.duck.IPovHardcore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(POVClip.class)
public class POVClipMixin implements IPovHardcore
{
    @Unique
    private final ValueBoolean hardcoreLook = new ValueBoolean("hardcore_look", false);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci)
    {
        ((POVClip) (Object) this).add(this.hardcoreLook);
    }

    @Override
    public ValueBoolean bbs_pov$getHardcoreLook()
    {
        return this.hardcoreLook;
    }
}
