package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.actions.ActionRecorder;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.chat.ChatActionClip;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ActionRecorder.class, remap = false)
public class ActionRecorderPovMixin
{
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void bbsPov$preventBakingChatActions(ActionClip clip, CallbackInfo ci)
    {
        if (clip instanceof ChatActionClip || clip instanceof CommandActionClip)
        {
            ci.cancel();
        }
    }
}
