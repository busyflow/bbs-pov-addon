package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.config.PovSettings;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BBSSettings.class, remap = false)
public class BBSSettingsPovMixin
{
    @Inject(
        method = "register",
        at = @At(
            value = "FIELD",
            target = "Lmchorse/bbs_mod/BBSSettings;recordingTeleport:Lmchorse/bbs_mod/settings/values/numeric/ValueBoolean;",
            shift = At.Shift.AFTER
        )
    )
    private static void bbsPov$registerPovCategory(SettingsBuilder builder, CallbackInfo info)
    {
        PovSettings.register(builder);
    }

    @Inject(method = "register", at = @At("RETURN"))
    private static void bbsPov$allowPovCameraMode(SettingsBuilder builder, CallbackInfo info)
    {
        ((BaseValueNumberAccessor) (Object) BBSSettings.editorCameraMode)
            .bbsPov$setMaximum(PovCameraMode.POV);
    }
}
