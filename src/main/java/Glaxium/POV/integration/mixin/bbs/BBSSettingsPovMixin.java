package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.config.PovSettings;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {BBSSettings.class},
   remap = false
)
public class BBSSettingsPovMixin {
   @Inject(
      method = {"register"},
      at = {@At(
         value = "FIELD",
         target = "Lmchorse/bbs_mod/BBSSettings;recordingTeleport:Lmchorse/bbs_mod/settings/values/numeric/ValueBoolean;",
         shift = Shift.AFTER
      )}
   )
   private static void bbsPov$registerPovCategory(SettingsBuilder builder, CallbackInfo info) {
      PovSettings.register(builder);
   }

   @Inject(
      method = {"register"},
      at = {@At("RETURN")}
   )
   private static void bbsPov$allowPovCameraMode(SettingsBuilder builder, CallbackInfo info) {
      ((BaseValueNumberAccessor)BBSSettings.editorCameraMode).bbsPov$setMaximum(6);
   }
}
