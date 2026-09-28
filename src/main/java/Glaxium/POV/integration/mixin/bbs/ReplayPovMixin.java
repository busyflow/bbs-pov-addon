package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.ReplayPovAccess;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Replay.class},
   remap = false
)
public class ReplayPovMixin implements ReplayPovAccess {
   @Unique
   private ValueBoolean bbsPov$overlayEnabled;
   @Unique
   private ValueBoolean bbsPov$hardcoreLook;
   @Unique
   private ValueBoolean bbsPov$cameraShake;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   private void bbsPov$addReplaySettings(String id, CallbackInfo info) {
      Replay replay = (Replay)this;
      this.bbsPov$overlayEnabled = new ValueBoolean("bbs_pov_enabled", false);
      this.bbsPov$hardcoreLook = new ValueBoolean("bbs_pov_hardcore_look", false);
      this.bbsPov$cameraShake = new ValueBoolean("bbs_pov_camera_shake", true);
      replay.add(this.bbsPov$overlayEnabled);
      replay.add(this.bbsPov$hardcoreLook);
      replay.add(this.bbsPov$cameraShake);
      if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHudData hotbar = access.bbsPov$getHud();
         if (hotbar != null) {
            hotbar.initializeReplayPreset(replay.keyframes);
         }

         RecordedHandData hand = access.bbsPov$getHand();
         if (hand != null) {
            hand.initializeReplayPreset();
         }
      }
   }

   @Redirect(
      method = {"applyClientActions"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/actions/types/ActionClip;applyClient(Lmchorse/bbs_mod/forms/entities/IEntity;Lmchorse/bbs_mod/film/Film;Lmchorse/bbs_mod/film/replays/Replay;I)V"
      )
   )
   private void bbsPov$filterRejectedUseAction(ActionClip action, IEntity entity, Film film, Replay replay, int tick) {
      PovPlaybackContext.Frame playback = PovPlaybackContext.getActive();
      if (action instanceof UseItemActionClip && playback != null && playback.replay() == replay && replay.keyframes instanceof ReplayKeyframesPovAccess access
         )
       {
         RecordedHandData hand = access.bbsPov$getHand();
         if (hand != null && (Integer)hand.activeHand.interpolate((float)tick, 0) == 0) {
            return;
         }
      }

      action.applyClient(entity, film, replay, tick);
   }

   @Override
   public ValueBoolean bbsPov$getOverlayEnabled() {
      return this.bbsPov$overlayEnabled;
   }

   @Override
   public ValueBoolean bbsPov$getHardcoreLook() {
      return this.bbsPov$hardcoreLook;
   }

   @Override
   public ValueBoolean bbsPov$getCameraShake() {
      return this.bbsPov$cameraShake;
   }
}
