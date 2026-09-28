package Glaxium.POV.integration.mixin.compat.iris;

import Glaxium.POV.hand.playback.PovHandPlayback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"net/irisshaders/iris/uniforms/CommonUniforms"},
   remap = false
)
public class IrisEyeBrightnessPovMixin {
   @Inject(
      method = {"getEyeBrightness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$cameraEyeBrightness(CallbackInfoReturnable<Vector2i> info) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && PovHandPlayback.isHandActive(client.getTickDelta())) {
         BlockPos position = client.gameRenderer.getCamera().getBlockPos();
         info.setReturnValue(new Vector2i(client.world.getLightLevel(LightType.BLOCK, position) * 16, client.world.getLightLevel(LightType.SKY, position) * 16));
      }
   }
}
