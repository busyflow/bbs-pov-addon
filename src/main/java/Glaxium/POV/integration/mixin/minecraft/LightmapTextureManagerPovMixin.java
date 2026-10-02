package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import Glaxium.POV.actions.screeneffect.render.PovNightVisionHelper;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerPovMixin
{
    @Inject(method = "getDarknessFactor", at = @At("HEAD"), cancellable = true)
    private void bbsPov$suppressDarkness(float delta, CallbackInfoReturnable<Float> info)
    {
        float povDarkness = PovDarknessHelper.resolveDarknessLightFactor(delta);
        if (povDarkness >= 0.0F)
        {
            info.setReturnValue(povDarkness);
            return;
        }

        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            info.setReturnValue(0.0F);
        }
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"
        )
    )
    private boolean bbsPov$hasStatusEffect(ClientPlayerEntity player, StatusEffect effect)
    {
        if (effect == StatusEffects.NIGHT_VISION)
        {
            float povNv = PovNightVisionHelper.resolveNightVisionStrength(0.0F);
            if (povNv >= 0.0F)
            {
                return povNv > 0.001F;
            }
        }
        return player.hasStatusEffect(effect);
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;getNightVisionStrength(Lnet/minecraft/entity/LivingEntity;F)F"
        )
    )
    private float bbsPov$getNightVisionStrength(LivingEntity entity, float delta)
    {
        float povNv = PovNightVisionHelper.resolveNightVisionStrength(delta);
        if (povNv >= 0.0F)
        {
            return povNv;
        }
        return GameRenderer.getNightVisionStrength(entity, delta);
    }
}

