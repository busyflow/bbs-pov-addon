package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.render.PovHandDepthManager;
import mchorse.bbs_mod.film.BaseFilmController;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {BaseFilmController.class},
   remap = false
)
public class BaseFilmControllerDepthPovMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureWorldBeforeReplays(WorldRenderContext context, CallbackInfo info) {
      PovHandDepthManager.onBeforeReplayRender();
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void bbsPov$captureReplayDepthAfterReplays(WorldRenderContext context, CallbackInfo info) {
      if (context != null && context.consumers() instanceof Immediate immediate) {
         immediate.draw();
      }

      PovHandDepthManager.captureReplayDepth();
   }
}
