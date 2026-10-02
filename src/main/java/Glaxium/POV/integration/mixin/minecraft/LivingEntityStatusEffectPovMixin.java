package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.screeneffect.render.PovBlindnessHelper;
import Glaxium.POV.actions.screeneffect.render.PovDarknessHelper;
import Glaxium.POV.actions.screeneffect.render.PovNightVisionHelper;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds POV action clip screen effects (Blindness, Darkness, Night Vision) to Minecraft and Iris shaders,
 * while suppressing unrelated world player effects from bleeding into BBS playback.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectPovMixin
{
    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true)
    private void bbsPov$suppressVisualStatusEffects(StatusEffect effect, CallbackInfoReturnable<Boolean> info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            float tickDelta = MinecraftClient.getInstance().getTickDelta();
            if (effect == StatusEffects.BLINDNESS)
            {
                float factor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
                info.setReturnValue(factor > 0.001F);
                return;
            }
            if (effect == StatusEffects.DARKNESS)
            {
                float factor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
                info.setReturnValue(factor > 0.001F);
                return;
            }
            if (effect == StatusEffects.NIGHT_VISION)
            {
                float factor = PovNightVisionHelper.resolveNightVisionStrength(tickDelta);
                info.setReturnValue(factor > 0.001F);
                return;
            }
            if (effect == StatusEffects.NAUSEA)
            {
                info.setReturnValue(false);
                return;
            }
        }
    }

    @Inject(method = "getStatusEffect", at = @At("HEAD"), cancellable = true)
    private void bbsPov$suppressVisualStatusEffectInstance(StatusEffect effect, CallbackInfoReturnable<StatusEffectInstance> info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            float tickDelta = MinecraftClient.getInstance().getTickDelta();
            if (effect == StatusEffects.BLINDNESS)
            {
                float factor = PovBlindnessHelper.resolveBlindnessFactor(tickDelta);
                info.setReturnValue(factor > 0.001F ? new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0, false, false, false) : null);
                return;
            }
            if (effect == StatusEffects.DARKNESS)
            {
                float factor = PovDarknessHelper.resolveDarknessFactor(tickDelta);
                info.setReturnValue(factor > 0.001F ? new StatusEffectInstance(StatusEffects.DARKNESS, 200, 0, false, false, false) : null);
                return;
            }
            if (effect == StatusEffects.NIGHT_VISION)
            {
                float factor = PovNightVisionHelper.resolveNightVisionStrength(tickDelta);
                info.setReturnValue(factor > 0.001F ? new StatusEffectInstance(StatusEffects.NIGHT_VISION, 200, 0, false, false, false) : null);
                return;
            }
            if (effect == StatusEffects.NAUSEA)
            {
                info.setReturnValue(null);
                return;
            }
        }
    }
}
