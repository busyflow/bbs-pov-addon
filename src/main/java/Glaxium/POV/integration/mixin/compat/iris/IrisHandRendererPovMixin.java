package Glaxium.POV.integration.mixin.compat.iris;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandDepthManager;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"net/irisshaders/iris/pathways/HandRenderer"},
   remap = false
)
public class IrisHandRendererPovMixin {
   @Inject(
      method = {"setupGlState"},
      at = {@At("TAIL")}
   )
   private void bbsPov$fixedHandProjection(GameRenderer renderer, Camera camera, MatrixStack matrices, float tickDelta, CallbackInfo info) {
      if (PovHandPlayback.isHandActive(tickDelta)) {
         Matrix4f projection = renderer.getBasicProjectionMatrix(70.0);
         int width = BBSRendering.getVideoWidth();
         int height = BBSRendering.getVideoHeight();
         if (width > 0 && height > 0) {
            projection.m00(projection.m11() / ((float)width / (float)height));
         }

         Matrix4f irisProjection = new Matrix4f().scaling(1.0F, 1.0F, 0.125F).mul(projection);
         renderer.loadProjectionMatrix(irisProjection);
         PovHandPicking.captureProjection(irisProjection);
         HandState state = PovHandPlayback.resolveActiveState(tickDelta);
         boolean worldInteraction = state != null && state.worldInteraction;
         boolean replayInteraction = state != null && state.replayInteraction;
         PovHandDepthManager.beginHandDepth(worldInteraction, replayInteraction);
      }
   }

   @Inject(
      method = {"canRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$forceCanRenderInFilm(Camera camera, GameRenderer gameRenderer, CallbackInfoReturnable<Boolean> info) {
      MinecraftClient client = MinecraftClient.getInstance();
      float tickDelta = client.getTickDelta();
      if (UIPovHandEditor.isActive()) {
         info.setReturnValue(false);
      } else if (PovHandPlayback.isHandActive(tickDelta)) {
         info.setReturnValue(true);
      } else {
         if (PovReplaySettings.getFilmPanel() != null || BBSModClient.getCameraController().getCurrent() != null || client.currentScreen instanceof UIScreen) {
            info.setReturnValue(false);
         }
      }
   }

   @Inject(
      method = {"renderSolid"},
      at = {@At("RETURN")}
   )
   private void bbsPov$endSolidHandDepth(
      MatrixStack matrices, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo info
   ) {
      if (PovHandPlayback.isHandActive(tickDelta)) {
         HandState state = PovHandPlayback.resolveActiveState(tickDelta);
         boolean worldInteraction = state != null && state.worldInteraction;
         boolean replayInteraction = state != null && state.replayInteraction;
         PovHandDepthManager.endHandDepth(worldInteraction, replayInteraction);
      }
   }

   @Inject(
      method = {"renderTranslucent"},
      at = {@At("RETURN")}
   )
   private void bbsPov$endTranslucentHandDepth(
      MatrixStack matrices, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline, CallbackInfo info
   ) {
      if (PovHandPlayback.isHandActive(tickDelta)) {
         HandState state = PovHandPlayback.resolveActiveState(tickDelta);
         boolean worldInteraction = state != null && state.worldInteraction;
         boolean replayInteraction = state != null && state.replayInteraction;
         PovHandDepthManager.endHandDepth(worldInteraction, replayInteraction);
      }
   }
}
