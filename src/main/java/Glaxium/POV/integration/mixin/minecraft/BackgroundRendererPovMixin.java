package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.render.PovBlindnessHelper;
import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import Glaxium.POV.utils.PovEffectSuppression;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BackgroundRenderer.class})
public abstract class BackgroundRendererPovMixin {
   @Shadow
   private static float red;
   @Shadow
   private static float green;
   @Shadow
   private static float blue;

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private static void bbsPov$darkenFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo info) {
      float darkFactor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
      float blindFactor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
      float totalFactor = Math.max(Math.max(0.0F, darkFactor), Math.max(0.0F, blindFactor));
      if (totalFactor > 0.001F) {
         float mult = 1.0F - totalFactor;
         red *= mult;
         green *= mult;
         blue *= mult;
         RenderSystem.clearColor(red, green, blue, 0.0F);
      }
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"
      )
   )
   private static boolean bbsPov$suppressHasStatusEffectRender(LivingEntity entity, StatusEffect effect) {
      return PovEffectSuppression.isBbsActive() ? false : entity.hasStatusEffect(effect);
   }

   @Redirect(
      method = {"applyFog"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"
      )
   )
   private static boolean bbsPov$suppressHasStatusEffectApplyFog(LivingEntity entity, StatusEffect effect) {
      return PovEffectSuppression.isBbsActive() ? false : entity.hasStatusEffect(effect);
   }

   @Inject(
      method = {"applyFog"},
      at = {@At("TAIL")}
   )
   private static void bbsPov$applyDarknessOrBlindnessFog(
      Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info
   ) {
      float blindFactor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
      if (blindFactor > 0.001F) {
         float radius = PovBlindnessHelper.resolveBlindnessRadius(tickDelta);
         float g = MathHelper.lerp(blindFactor, viewDistance * 0.75F, radius);
         float fogStart = fogType == FogType.FOG_SKY ? 0.0F : g * 0.25F;
         RenderSystem.setShaderFogStart(fogStart);
         RenderSystem.setShaderFogEnd(g);
         RenderSystem.setShaderFogShape(FogShape.SPHERE);
      } else {
         float darkFactor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
         if (darkFactor > 0.001F) {
            float radius = PovDarknessHelper.resolveDarknessRadius(tickDelta);
            float g = MathHelper.lerp(darkFactor, viewDistance * 0.75F, radius);
            float fogStart = fogType == FogType.FOG_SKY ? 0.0F : g * 0.75F;
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(g);
            RenderSystem.setShaderFogShape(FogShape.SPHERE);
         }
      }
   }
}
