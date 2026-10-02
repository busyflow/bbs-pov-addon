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

/** Both eyeBrightness and its smoothed shader uniform use this supplier. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public class IrisEyeBrightnessPovMixin
{
    @Inject(method = "getEyeBrightness", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$cameraEyeBrightness(CallbackInfoReturnable<Vector2i> info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && PovHandPlayback.isHandActive(client.getTickDelta()))
        {
            net.minecraft.util.math.Vec3d camPos = client.gameRenderer.getCamera().getPos();
            BlockPos position = BlockPos.ofFloored(camPos);
            if (!client.world.isAir(position) && client.world.isAir(position.up()))
            {
                position = position.up();
            }
            int blockLight = client.world.getLightLevel(LightType.BLOCK, position);
            int skyLight = client.world.getLightLevel(LightType.SKY, position);
            info.setReturnValue(new Vector2i(blockLight * 16, skyLight * 16));
        }
    }

    @Inject(method = "getDarknessFactor", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$darknessFactor(CallbackInfoReturnable<Float> info)
    {
        float delta = MinecraftClient.getInstance().getTickDelta();
        float factor = Glaxium.POV.actions.screeneffect.render.PovDarknessHelper.resolveDarknessFactor(delta);
        if (factor >= 0.0F)
        {
            info.setReturnValue(factor);
        }
    }

    @Inject(method = "getBlindness", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$blindnessUniform(CallbackInfoReturnable<Float> info)
    {
        float delta = MinecraftClient.getInstance().getTickDelta();
        float blind = Glaxium.POV.actions.screeneffect.render.PovBlindnessHelper.resolveBlindnessFactor(delta);
        if (blind >= 0.0F)
        {
            info.setReturnValue(blind);
            return;
        }
    }
}
