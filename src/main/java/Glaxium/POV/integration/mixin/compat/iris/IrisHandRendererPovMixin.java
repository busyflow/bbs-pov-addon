package Glaxium.POV.integration.mixin.compat.iris;

import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When Iris shaders are active, Iris's HandRenderer.canRender() skips hand rendering
 * if hudHidden is true, if in spectator mode, if in third-person camera, or if
 * GameRenderer.renderHand is disabled.
 *
 * This mixin forces canRender() to return true when a BBS Film or POV playback is active,
 * allowing Iris to execute its GBuffer hand pass with full shader lighting and shadows.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public class IrisHandRendererPovMixin
{
    /** Iris bypasses GameRenderer.renderHand for both shader hand passes. */
    @Inject(method = "setupGlState", at = @At("TAIL"))
    private void bbsPov$fixedHandProjection(CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();
        if (!PovHandPlayback.isHandActive(tickDelta))
        {
            return;
        }

        PovHandPlayback.begin(tickDelta);

        GameRenderer renderer = client.gameRenderer;
        Matrix4f projection = renderer.getBasicProjectionMatrix(70D);
        int width = BBSRendering.isCustomSize() ? BBSRendering.getVideoWidth() : client.getWindow().getFramebufferWidth();
        int height = BBSRendering.isCustomSize() ? BBSRendering.getVideoHeight() : client.getWindow().getFramebufferHeight();
        if (width > 0 && height > 0)
        {
            projection.m00(projection.m11() / (width / (float) height));
        }

        HandState state = PovHandPlayback.getActiveState();
        boolean worldInteraction = state != null && state.worldInteraction;
        boolean replayInteraction = state != null && state.replayInteraction;

        Matrix4f irisProjection = new Matrix4f().scaling(1F, 1F, 0.125F).mul(projection);
        renderer.loadProjectionMatrix(irisProjection);
        PovHandPicking.captureProjection(projection);

        if (!Glaxium.POV.hand.render.PovHandDepthManager.hasSavedSceneDepth())
        {
            Glaxium.POV.hand.render.PovHandDepthManager.prepareIrisHandDepth(worldInteraction, replayInteraction);
        }
    }

    @Inject(method = "canRender", at = @At("HEAD"), cancellable = true)
    private void bbsPov$forceCanRenderInFilm(CallbackInfoReturnable<Boolean> info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();

        if (Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            info.setReturnValue(false);
            return;
        }

        if (Glaxium.POV.hand.playback.PovHandPlayback.isHandActive(tickDelta))
        {
            info.setReturnValue(true);
            return;
        }

        if (PovReplaySettings.getFilmPanel() != null
            || mchorse.bbs_mod.BBSModClient.getCameraController().getCurrent() != null
            || client.currentScreen instanceof mchorse.bbs_mod.ui.framework.UIScreen)
        {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "isRenderingSolid", at = @At("HEAD"), cancellable = true)
    private void bbsPov$overrideIsRenderingSolid(CallbackInfoReturnable<Boolean> info)
    {
        if (PovHandPicking.isStencilPass() || mchorse.bbs_mod.utils.iris.IrisUtils.isRenderingOffscreen())
        {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "isHandTranslucent", at = @At("HEAD"), cancellable = true)
    private void bbsPov$overrideIsHandTranslucent(CallbackInfoReturnable<Boolean> info)
    {
        if (PovHandPicking.isStencilPass() || mchorse.bbs_mod.utils.iris.IrisUtils.isRenderingOffscreen())
        {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "renderSolid", at = @At("HEAD"))
    private void bbsPov$beginSolidHand(CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();
        if (PovHandPlayback.isHandActive(tickDelta) && !PovHandPlayback.isActive())
        {
            PovHandPlayback.begin(tickDelta);
        }
    }

    @Inject(method = "renderSolid", at = @At("RETURN"))
    private void bbsPov$endSolidHand(CallbackInfo info)
    {
        HandState state = PovHandPlayback.getActiveState();
        boolean worldInteraction = state != null && state.worldInteraction;
        boolean replayInteraction = state != null && state.replayInteraction;
        if (Glaxium.POV.hand.render.PovHandDepthManager.hasSavedSceneDepth()
            && !net.irisshaders.iris.pathways.HandRenderer.INSTANCE.isAnyHandTranslucent())
        {
            Glaxium.POV.hand.render.PovHandDepthManager.endIrisHandDepth(worldInteraction, replayInteraction);
        }
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"))
    private void bbsPov$beginTranslucentHand(CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();
        if (PovHandPlayback.isHandActive(tickDelta) && !PovHandPlayback.isActive())
        {
            PovHandPlayback.begin(tickDelta);
        }
    }

    @Inject(method = "renderTranslucent", at = @At("RETURN"))
    private void bbsPov$endTranslucentHand(CallbackInfo info)
    {
        HandState state = PovHandPlayback.getActiveState();
        boolean worldInteraction = state != null && state.worldInteraction;
        boolean replayInteraction = state != null && state.replayInteraction;
        if (Glaxium.POV.hand.render.PovHandDepthManager.hasSavedSceneDepth())
        {
            Glaxium.POV.hand.render.PovHandDepthManager.endIrisHandDepth(worldInteraction, replayInteraction);
        }
        if (PovHandPlayback.isActive())
        {
            PovHandPlayback.end();
        }
    }
}
