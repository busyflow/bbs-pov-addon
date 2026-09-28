package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.toast.recording.ToastRecorder;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ToastManager.class})
public class ToastManagerPovMixin {
   @Inject(
      method = {"add"},
      at = {@At("HEAD")}
   )
   private void bbsPov$onToastAdded(Toast toast, CallbackInfo ci) {
      if (PovSettings.isBakeAnyActions()) {
         try {
            Recorder recorder = BBSModClient.getFilms().getRecorder();
            if (recorder != null && !recorder.hasNotStarted() && recorder.tick >= 0 && recorder.keyframes instanceof ReplayKeyframesPovAccess access) {
               RecordedPovActions actions = access.bbsPov$getActions();
               if (actions != null) {
                  ToastRecorder.onToastAdded(toast, actions, recorder.tick);
               }
            }
         } catch (Throwable var6) {
         }
      }
   }
}
