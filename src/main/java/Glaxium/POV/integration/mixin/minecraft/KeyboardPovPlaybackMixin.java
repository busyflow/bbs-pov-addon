package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackInput;
import net.minecraft.client.Keyboard;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Keyboard.class, priority = 1100)
public abstract class KeyboardPovPlaybackMixin
{
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void bbsPov$lockPlaybackKeys(long window, int key, int scancode, int action,
        int modifiers, CallbackInfo info)
    {
        /* Only block press and repeat events. Releasing keys must always pass through
         * so Minecraft can properly clear pressed states and never get stuck. */
        if (action != GLFW.GLFW_RELEASE && PovPlaybackInput.isLocked() && !PovPlaybackInput.isAllowed(key, scancode))
        {
            info.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void bbsPov$lockPlaybackText(long window, int codePoint, int modifiers, CallbackInfo info)
    {
        if (PovPlaybackInput.isLocked())
        {
            info.cancel();
        }
    }
}
