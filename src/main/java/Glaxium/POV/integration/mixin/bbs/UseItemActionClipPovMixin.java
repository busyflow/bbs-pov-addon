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

/** The server/fake-player half of Right-Control playback executes use-item
 * clips independently from Replay.applyClientActions. Reject clips that have
 * no matching POV active-use keyframe before they can start item use or emit
 * consumption particles. */
@Mixin(value = UseItemActionClip.class, remap = false)
public abstract class UseItemActionClipPovMixin
{
    @Inject(method = "applyAction", at = @At("HEAD"), cancellable = true)
    private void bbsPov$filterRejectedUse(
        LivingEntity actor,
        SuperFakePlayer fakePlayer,
        Film film,
        Replay replay,
        int tick,
        CallbackInfo info)
    {
        if (!(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        RecordedHandData hand = access.bbsPov$getHand();
        boolean mainHand = ((UseItemActionClip) (Object) this).hand.get();
        int expected = mainHand ? 1 : 2;
        int active = hand == null ? 0 : hand.activeHand.interpolate(tick, 0);

        if (active != expected)
        {
            info.cancel();
        }
    }
}
