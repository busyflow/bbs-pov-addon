package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Keyframe.class, remap = false)
public abstract class KeyframePovMixin
{
    @Inject(method = "setTick(FZ)V", at = @At("HEAD"))
    private void bbsPov$moveGroupedSlotKeyframes(float tick, boolean notify, CallbackInfo info)
    {
        Keyframe<?> keyframe = (Keyframe<?>) (Object) this;

        if (Math.abs(keyframe.getTick() - tick) >= 0.0001F
            && keyframe.getParent() instanceof KeyframeChannel<?> channel
            && channel.getParent() instanceof GuiPovActionClip clip
            && clip.isSlotAnchor(channel))
        {
            clip.moveSlotKeyframes(channel, keyframe.getTick(), tick);
        }
    }
}
