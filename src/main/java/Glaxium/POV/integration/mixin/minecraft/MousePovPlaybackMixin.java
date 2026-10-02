package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackInput;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Mouse.class, priority = 1100)
public abstract class MousePovPlaybackMixin
{
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void bbsPov$lockPlaybackMouseButtons(long window, int button, int action, int mods, CallbackInfo info)
    {
        if (action != 0 && PovPlaybackInput.isFirstPersonPlayback()) // 0 is GLFW_RELEASE
        {
            info.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void bbsPov$lockPlaybackMouseScroll(long window, double horizontal, double vertical, CallbackInfo info)
    {
        if (PovPlaybackInput.isFirstPersonPlayback())
        {
            info.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void bbsPov$lockPlaybackMouseMove(CallbackInfo info)
    {
        if (PovPlaybackInput.isFirstPersonPlayback())
        {
            info.cancel();
        }
    }
}
