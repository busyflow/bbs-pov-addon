package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.item.UseItemActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UseItemActionClip.class},
   remap = false
)
public abstract class UseItemActionClipPovMixin {
   @Inject(
      method = {"applyAction"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$filterRejectedUse(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, Replay replay, int tick, CallbackInfo info) {
      if (replay.keyframes instanceof ReplayKeyframesPovAccess access) {
         RecordedHandData var12 = access.bbsPov$getHand();
         boolean mainHand = (Boolean)((UseItemActionClip)(Object)this).hand.get();
         int expected = mainHand ? 1 : 2;
         int active = var12 == null ? 0 : (Integer)var12.activeHand.interpolate((float)tick, 0);
         if (active != expected) {
            info.cancel();
         }
      }
   }
}
