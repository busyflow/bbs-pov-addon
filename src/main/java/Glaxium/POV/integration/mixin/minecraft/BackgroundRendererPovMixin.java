package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.render.PovBlindnessHelper;
import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import Glaxium.POV.actions.screeneffect.render.PovNightVisionHelper;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererPovMixin
{
    @org.spongepowered.asm.mixin.Shadow private static float red;
    @org.spongepowered.asm.mixin.Shadow private static float green;
    @org.spongepowered.asm.mixin.Shadow private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void bbsPov$darkenFogColor(
        Camera camera,
        float tickDelta,
        net.minecraft.client.world.ClientWorld world,
        int viewDistance,
        float skyDarkness,
        CallbackInfo info)
    {
        float darkFactor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
        float blindFactor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
        float totalFactor = Math.max(Math.max(0.0F, darkFactor), Math.max(0.0F, blindFactor));
        if (totalFactor > 0.001F)
        {
            float mult = 1.0F - totalFactor;
            red *= mult;
            green *= mult;
            blue *= mult;
            RenderSystem.clearColor(red, green, blue, 0.0F);
        }
    }



    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void bbsPov$applyDarknessOrBlindnessFog(
        Camera camera,
        BackgroundRenderer.FogType fogType,
        float viewDistance,
        boolean thickFog,
        float tickDelta,
        CallbackInfo info)
    {
        float blindFactor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
        if (blindFactor > 0.001F)
        {
            float radius = PovBlindnessHelper.resolveBlindnessRadius(tickDelta);
            float g = MathHelper.lerp(blindFactor, viewDistance * 0.75F, radius);
            float fogStart = fogType == BackgroundRenderer.FogType.FOG_SKY ? 0.0F : g * 0.25F;
            float fogEnd = g;
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogShape(FogShape.SPHERE);
            return;
        }

        float darkFactor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
        if (darkFactor > 0.001F)
        {
            float radius = PovDarknessHelper.resolveDarknessRadius(tickDelta);
            float g = MathHelper.lerp(darkFactor, viewDistance, radius);
            float fogStart = fogType == BackgroundRenderer.FogType.FOG_SKY ? 0.0F : g * 0.75F;
            float fogEnd = g;
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogShape(FogShape.SPHERE);
        }
    }
}

