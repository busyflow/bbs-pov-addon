package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public abstract class PlayerEntityPovMixin {
   @Inject(
      method = {"getName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$getMorphedName(CallbackInfoReturnable<Text> cir) {
      String replayName = ChatMorphHelper.getActiveReplayName();
      if (replayName != null && !replayName.isEmpty()) {
         cir.setReturnValue(Text.literal(replayName));
      } else {
         PlayerEntity self = (PlayerEntity)this;
         String morphName = ChatMorphHelper.getPlayerMorphName(self);
         if (morphName != null && !morphName.isEmpty()) {
            cir.setReturnValue(Text.literal(morphName));
         }
      }
   }

   @Inject(
      method = {"getDisplayName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$getMorphedDisplayName(CallbackInfoReturnable<Text> cir) {
      String replayName = ChatMorphHelper.getActiveReplayName();
      if (replayName != null && !replayName.isEmpty()) {
         cir.setReturnValue(Text.literal(replayName));
      } else {
         PlayerEntity self = (PlayerEntity)this;
         String morphName = ChatMorphHelper.getPlayerMorphName(self);
         if (morphName != null && !morphName.isEmpty()) {
            cir.setReturnValue(Text.literal(morphName));
         }
      }
   }
}
