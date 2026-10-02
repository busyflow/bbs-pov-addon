package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.camera.CameraShakeApplier;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * During POV playback / gated editor preview, cancel leftover vanilla hurt tilt
 * and apply baked Camera Shake clips. Priority 900 runs after BBS (1000) so film
 * camera roll is written first, then shake is layered on top.
 */
@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererHurtPovMixin
{
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void bbsPov$applyPovCameraShake(MatrixStack matrices, float tickDelta, CallbackInfo info)
    {
        if (CameraShakeApplier.shouldCancelVanilla(tickDelta))
        {
            info.cancel();
            CameraShakeApplier.apply(matrices, tickDelta);
        }
    }
}
