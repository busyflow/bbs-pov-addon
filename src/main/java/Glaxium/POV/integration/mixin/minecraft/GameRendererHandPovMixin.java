package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandDepthManager;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GameRenderer.class},
   priority = 200
)
public abstract class GameRendererHandPovMixin {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private BufferBuilderStorage buffers;
   @Shadow
   @Final
   private LightmapTextureManager lightmapTextureManager;
   @Shadow
   @Final
   public HeldItemRenderer firstPersonRenderer;

   @Inject(
      method = {"renderHand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$renderHandBeforeBbsCancellation(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo info) {
      if (UIPovHandEditor.isActive()) {
         info.cancel();
      } else if (BBSRendering.isIrisShadersEnabled() && PovHandPlayback.isHandActive(tickDelta)) {
         info.cancel();
      } else if (!PovHandPlayback.begin(tickDelta)) {
         if (PovPlaybackContext.getActive() != null
            || PovReplaySettings.getFilmPanel() != null
            || BBSModClient.getCameraController().getCurrent() != null
            || this.client.currentScreen instanceof UIScreen) {
            info.cancel();
         }
      } else {
         HandState state = PovHandPlayback.getActiveState();
         boolean worldInteraction = state != null && state.worldInteraction;
         boolean replayInteraction = state != null && state.replayInteraction;
         ClientPlayerEntity player = this.client.player;
         if (player == null) {
            PovHandPlayback.end();
         } else {
            GameRenderer renderer = (GameRenderer)(Object)this;
            double fov = 70.0;
            Matrix4f handProjection = renderer.getBasicProjectionMatrix(fov);
            int videoWidth = BBSRendering.getVideoWidth();
            int videoHeight = BBSRendering.getVideoHeight();
            boolean inHandEditor = UIPovHandEditor.isActive();
            Area editorFrame = inHandEditor && UIPovHandEditor.getActive() != null ? UIPovHandEditor.getActive().getFrameArea() : null;
            if (inHandEditor && editorFrame != null) {
               RenderSystem.clearColor(0.094F, 0.094F, 0.094F, 1.0F);
               RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
               double scale = this.client.getWindow().getScaleFactor();
               int vx = (int)Math.round((double)editorFrame.x * scale);
               int vy = (int)Math.round((double)(this.client.getWindow().getScaledHeight() - (editorFrame.y + editorFrame.h)) * scale);
               int vw = (int)Math.round((double)editorFrame.w * scale);
               int vh = (int)Math.round((double)editorFrame.h * scale);
               RenderSystem.viewport(vx, vy, vw, vh);
               handProjection.setPerspective((float)Math.toRadians(fov), 1.7777778F, 0.05F, 100.0F);
               renderer.loadProjectionMatrix(handProjection);
            } else {
               if (videoWidth > 0 && videoHeight > 0) {
                  float aspect = (float)videoWidth / (float)videoHeight;
                  handProjection.m00(handProjection.m11() / aspect);
                  renderer.loadProjectionMatrix(handProjection);
               } else {
                  renderer.loadProjectionMatrix(handProjection);
               }

               PovHandDepthManager.prepareDepthForHand(worldInteraction, replayInteraction);
            }

            PovHandPicking.captureProjection(RenderSystem.getProjectionMatrix());
            matrices.push();
            matrices.loadIdentity();
            this.lightmapTextureManager.enable();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(515);

            try {
               Immediate consumers = this.buffers.getEntityVertexConsumers();
               int light = this.client.world == null ? 15728880 : WorldRenderer.getLightmapCoordinates(this.client.world, camera.getBlockPos());
               PovHandPlayback.render(this.firstPersonRenderer, tickDelta, matrices, consumers, player, light);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.depthFunc(515);
               consumers.draw();
               PovHandGizmo.captureVisual();
            } finally {
               if (inHandEditor) {
                  RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
                  RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
               }

               PovHandDepthManager.endHandDepth(worldInteraction, replayInteraction);
               this.lightmapTextureManager.disable();
               matrices.pop();
               PovHandPlayback.end();
            }

            info.cancel();
         }
      }
   }
}
