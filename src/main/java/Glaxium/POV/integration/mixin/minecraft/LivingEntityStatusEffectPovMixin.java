package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.utils.PovEffectSuppression;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class LivingEntityStatusEffectPovMixin {
   @Inject(
      method = {"hasStatusEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$suppressVisualStatusEffects(StatusEffect effect, CallbackInfoReturnable<Boolean> info) {
      if (PovEffectSuppression.isBbsActive()
         && (effect == StatusEffects.BLINDNESS || effect == StatusEffects.DARKNESS || effect == StatusEffects.NIGHT_VISION || effect == StatusEffects.NAUSEA)) {
         info.setReturnValue(false);
      }
   }

   @Inject(
      method = {"getStatusEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$suppressVisualStatusEffectInstance(StatusEffect effect, CallbackInfoReturnable<StatusEffectInstance> info) {
      if (PovEffectSuppression.isBbsActive()
         && (effect == StatusEffects.BLINDNESS || effect == StatusEffects.DARKNESS || effect == StatusEffects.NIGHT_VISION || effect == StatusEffects.NAUSEA)) {
         info.setReturnValue(null);
      }
   }
}
