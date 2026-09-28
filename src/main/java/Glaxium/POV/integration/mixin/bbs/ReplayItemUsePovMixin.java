package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.cubic.animation.ItemUsePose.Use;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayItemUse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ReplayItemUse.class},
   remap = false
)
public abstract class ReplayItemUsePovMixin {
   @Inject(
      method = {"compute"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$useRecordedHandState(Replay replay, float tick, boolean mainHand, CallbackInfoReturnable<Use> info) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive();
      boolean povPlayback = playback != null && playback.replay() == replay;
      boolean filmPreview = PovReplaySettings.getSelectedReplay() == replay;
      if (povPlayback || filmPreview) {
         if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
            RecordedHandData hand = access.bbsPov$getHand();
            if (hand == null) {
               info.setReturnValue(null);
            } else {
               int active = (Integer)hand.activeHand.interpolate((float)Math.floor((double)tick), 0);
               int expected = mainHand ? 1 : 2;
               if (active != expected) {
                  info.setReturnValue(null);
               }
            }
         }
      }
   }
}
