package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerManager.class})
public class PlayerManagerPovMixin {
   @Shadow
   @Final
   private List<ServerPlayerEntity> players;

   @Inject(
      method = {"getPlayer(Ljava/lang/String;)Lnet/minecraft/server/network/ServerPlayerEntity;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$getPlayerByMorphOrReplay(String name, CallbackInfoReturnable<ServerPlayerEntity> cir) {
      if (cir.getReturnValue() == null && name != null && !name.isEmpty()) {
         for (ServerPlayerEntity player : this.players) {
            String replayName = ChatMorphHelper.getActiveReplayName();
            if (replayName != null && replayName.equalsIgnoreCase(name)) {
               cir.setReturnValue(player);
               return;
            }

            String morphName = ChatMorphHelper.getPlayerMorphName(player);
            if (morphName != null && morphName.equalsIgnoreCase(name)) {
               cir.setReturnValue(player);
               return;
            }

            for (String rName : ChatMorphHelper.getAllFilmReplayNames()) {
               if (rName.equalsIgnoreCase(name)) {
                  cir.setReturnValue(player);
                  return;
               }
            }
         }
      }
   }

   @Inject(
      method = {"getPlayerNames"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$getPlayerNames(CallbackInfoReturnable<String[]> cir) {
      List<String> result = new ArrayList<>();
      String replayName = ChatMorphHelper.getActiveReplayName();

      for (ServerPlayerEntity player : this.players) {
         if (replayName != null && !replayName.isEmpty()) {
            result.add(replayName);
         } else {
            String morphName = ChatMorphHelper.getPlayerMorphName(player);
            if (morphName != null && !morphName.isEmpty()) {
               result.add(morphName);
            } else {
               result.add(player.getGameProfile().getName());
            }
         }
      }

      Iterator var10 = ChatMorphHelper.getAllFilmReplayNames().iterator();

      while (true) {
         String rName;
         boolean isPlayerMorph;
         do {
            if (!var10.hasNext()) {
               if (replayName != null && !replayName.isEmpty()) {
                  for (ServerPlayerEntity playerx : this.players) {
                     String morphName = ChatMorphHelper.getPlayerMorphName(playerx);
                     if (morphName != null && !morphName.equalsIgnoreCase(replayName)) {
                        result.removeIf(n -> n.equalsIgnoreCase(morphName));
                     }
                  }
               }

               cir.setReturnValue(result.toArray(new String[0]));
               return;
            }

            rName = (String)var10.next();
            if (replayName == null || replayName.isEmpty()) {
               break;
            }

            isPlayerMorph = false;

            for (ServerPlayerEntity playerxx : this.players) {
               String morphName = ChatMorphHelper.getPlayerMorphName(playerxx);
               if (morphName != null && rName.equalsIgnoreCase(morphName)) {
                  isPlayerMorph = true;
                  break;
               }
            }
         } while (isPlayerMorph);

         if (!result.contains(rName)) {
            result.add(rName);
         }
      }
   }
}
