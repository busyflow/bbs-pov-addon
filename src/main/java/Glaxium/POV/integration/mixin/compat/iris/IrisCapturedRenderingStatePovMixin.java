package Glaxium.POV.integration.mixin.compat.iris;

import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CapturedRenderingState", remap = false)
public class IrisCapturedRenderingStatePovMixin
{
    @Inject(method = "getDarknessLightFactor", at = @At("HEAD"), cancellable = true)
    private void bbsPov$getDarknessLightFactor(CallbackInfoReturnable<Float> info)
    {
        float delta = MinecraftClient.getInstance().getTickDelta();
        float factor = PovDarknessHelper.resolveDarknessLightFactor(delta);
        if (factor >= 0.0F)
        {
            info.setReturnValue(factor);
        }
    }
}
