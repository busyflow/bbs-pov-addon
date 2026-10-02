package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.recording.PovRecordingSession;
import Glaxium.POV.render.PovOverlayRenderer;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Right-Control POV overlay into BBS's film framebuffer before
 * BBS copies that framebuffer to the screen/export texture. InGameHud is too
 * late for this path and is cancelled by BBS while PlayCameraController runs. */
@Mixin(value = BBSRendering.class, remap = false)
public abstract class BBSRenderingPovMixin
{
    @Inject(method = "onWorldRenderBegin", at = @At("HEAD"))
    private static void bbsPov$beginOverlayFrame(CallbackInfo info)
    {
        PovOverlayRenderer.beginFrame();
        Glaxium.POV.hand.render.PovHandDepthManager.onRenderWorldStart();
    }

    @Inject(method = "onWorldRenderEnd", at = @At("HEAD"))
    private static void bbsPov$renderPlaybackOverlay(CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        float tickDelta = client.getTickDelta();

        PovRecordingSession.sampleCursor(tickDelta);

        if (PovPlaybackContext.getActive(tickDelta) != null)
        {
            DrawContext context = new DrawContext(
                client,
                client.getBufferBuilders().getEntityVertexConsumers());

            PovOverlayRenderer.renderPlayback(new Batcher2D(context), tickDelta);
            context.draw();
            return;
        }
        mchorse.bbs_mod.ui.film.UIFilmPanel filmPanel = Glaxium.POV.render.PovViewportMetrics.resolveFilmPanel();
        if (filmPanel != null)
        {
            DrawContext context = new DrawContext(
                client,
                client.getBufferBuilders().getEntityVertexConsumers());

            PovOverlayRenderer.render(context.getMatrices(), new Batcher2D(context));
            context.draw();
            return;
        }

        PovRecordingSession.sampleCursor(tickDelta);
        if (mchorse.bbs_mod.BBSModClient.getVideoRecorder() != null
            && mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording()
            && client.player != null
            && ((client.currentScreen != null && !(client.currentScreen instanceof mchorse.bbs_mod.ui.framework.UIScreen)) || client.player.getSleepTimer() > 0))
        {
            DrawContext context = new DrawContext(
                client,
                client.getBufferBuilders().getEntityVertexConsumers());

            Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer.render(new Batcher2D(context), tickDelta);
            context.draw();
        }
    }
}
