package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.recording.PovRecordingSession;
import Glaxium.POV.render.PovOverlayRenderer;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {BBSRendering.class},
   remap = false
)
public abstract class BBSRenderingPovMixin {
   @Inject(
      method = {"onWorldRenderBegin"},
      at = {@At("HEAD")}
   )
   private static void bbsPov$beginOverlayFrame(CallbackInfo info) {
      PovOverlayRenderer.beginFrame();
   }

   @Inject(
      method = {"onWorldRenderEnd"},
      at = {@At("HEAD")}
   )
   private static void bbsPov$renderPlaybackOverlay(CallbackInfo info) {
      MinecraftClient client = MinecraftClient.getInstance();
      float tickDelta = client.getTickDelta();
      PovRecordingSession.sampleCursor(tickDelta);
      if (PovPlaybackContext.getActive(tickDelta) != null) {
         DrawContext context = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());
         PovOverlayRenderer.renderPlayback(new Batcher2D(context), tickDelta);
         context.draw();
      } else {
         if (BBSModClient.getVideoRecorder() != null
            && BBSModClient.getVideoRecorder().isRecording()
            && client.player != null
            && (client.currentScreen != null || client.player.getSleepTimer() > 0)) {
            DrawContext context = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());
            LiveGuiPreviewRenderer.render(new Batcher2D(context), tickDelta);
            context.draw();
         }
      }
   }
}
