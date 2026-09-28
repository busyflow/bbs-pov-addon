package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.render.PovBlindnessHelper;
import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import Glaxium.POV.hand.render.PovHandDepthManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class WorldRendererPovMixin {
   @Inject(
      method = {"renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$suppressSkyAndStarsOnDarkness(
      MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo info
   ) {
      float darkFactor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
      float blindFactor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
      if (darkFactor > 0.05F || blindFactor > 0.05F) {
         info.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void bbsPov$onRenderWorldStart(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f projectionMatrix,
      CallbackInfo info
   ) {
      PovHandDepthManager.onRenderWorldStart();
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void bbsPov$onRenderWorldEnd(
      MatrixStack matrices,
      float tickDelta,
      long limitTime,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightmapTextureManager lightmapTextureManager,
      Matrix4f projectionMatrix,
      CallbackInfo info
   ) {
      PovHandDepthManager.onRenderWorldEnd();
   }
}
