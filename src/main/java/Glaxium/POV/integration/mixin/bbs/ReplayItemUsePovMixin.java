package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.cubic.animation.ItemUsePose;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayItemUse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevent BBS's raw action-clip scanner from forcing the real client player
 * into an item-use pose when the POV recording says no item is being used. */
@Mixin(value = ReplayItemUse.class, remap = false)
public abstract class ReplayItemUsePovMixin
{
    @Inject(method = "compute", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$useRecordedHandState(
        Replay replay,
        float tick,
        boolean mainHand,
        CallbackInfoReturnable<ItemUsePose.Use> info)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive();
        boolean povPlayback = playback != null && playback.replay() == replay;
        boolean filmPreview = PovReplaySettings.getSelectedReplay() == replay;

        if (!povPlayback && !filmPreview)
        {
            return;
        }

        if (!(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        RecordedHandData hand = access.bbsPov$getHand();

        if (hand == null)
        {
            info.setReturnValue(null);
            return;
        }

        int active = hand.activeHand.interpolate((float) Math.floor(tick), 0);
        int expected = mainHand ? 1 : 2;

        if (active != expected)
        {
            info.setReturnValue(null);
        }
    }
}
