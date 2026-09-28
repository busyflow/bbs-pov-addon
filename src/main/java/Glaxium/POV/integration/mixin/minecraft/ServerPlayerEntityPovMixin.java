package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import Glaxium.POV.playback.PovPlayerProtection;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayerEntity.class})
public abstract class ServerPlayerEntityPovMixin {
   @Inject(
      method = {"damage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$cancelPlaybackDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
      if (PovPlayerProtection.contains((ServerPlayerEntity)this)) {
         info.setReturnValue(false);
      }
   }

   @Inject(
      method = {"getPlayerListName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$getMorphedPlayerListName(CallbackInfoReturnable<Text> cir) {
      String replayName = ChatMorphHelper.getActiveReplayName();
      if (replayName != null && !replayName.isEmpty()) {
         cir.setReturnValue(Text.literal(replayName));
      } else {
         ServerPlayerEntity self = (ServerPlayerEntity)this;
         String morphName = ChatMorphHelper.getPlayerMorphName(self);
         if (morphName != null && !morphName.isEmpty()) {
            cir.setReturnValue(Text.literal(morphName));
         }
      }
   }
}
