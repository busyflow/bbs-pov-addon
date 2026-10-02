package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.render.PovHandDepthManager;
import mchorse.bbs_mod.film.BaseFilmController;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseFilmController.class, remap = false)
public class BaseFilmControllerDepthPovMixin
{
    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$captureWorldBeforeReplays(WorldRenderContext context, CallbackInfo info)
    {
        if (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.utils.iris.IrisUtils.isShadowPass())
        {
            return;
        }
        float tickDelta = context != null ? context.tickDelta() : 0F;
        if (Glaxium.POV.hand.playback.PovHandPlayback.isHandActive(tickDelta))
        {
            PovHandDepthManager.onBeforeReplayRender();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void bbsPov$captureReplayDepthAfterReplays(WorldRenderContext context, CallbackInfo info)
    {
        if (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.utils.iris.IrisUtils.isShadowPass())
        {
            return;
        }
        float tickDelta = context != null ? context.tickDelta() : 0F;
        if (Glaxium.POV.hand.playback.PovHandPlayback.isHandActive(tickDelta))
        {
            if (context != null && context.consumers() instanceof VertexConsumerProvider.Immediate immediate)
            {
                immediate.draw();
            }
            PovHandDepthManager.captureReplayDepth();
        }
    }
}
