package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientCommandSource.class})
public class ClientCommandSourcePovMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(
      method = {"getPlayerNames"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$replacePlayerNames(CallbackInfoReturnable<Collection<String>> cir) {
      Collection<String> original = (Collection<String>)cir.getReturnValue();
      if (original != null) {
         String playerName = this.client.player != null && this.client.player.getGameProfile() != null ? this.client.player.getGameProfile().getName() : null;
         String morphName = this.client.player != null ? ChatMorphHelper.getPlayerMorphName(this.client.player) : null;
         String replayName = ChatMorphHelper.getActiveReplayName();
         List<String> modified = new ArrayList<>();

         for (String name : original) {
            if (playerName != null && name.equalsIgnoreCase(playerName)) {
               if (replayName != null && !replayName.isEmpty()) {
                  if (!modified.contains(replayName)) {
                     modified.add(replayName);
                  }
               } else if (morphName != null && !morphName.isEmpty()) {
                  if (!modified.contains(morphName)) {
                     modified.add(morphName);
                  }
               } else if (!modified.contains(name)) {
                  modified.add(name);
               }
            } else if (morphName != null && name.equalsIgnoreCase(morphName)) {
               if (replayName != null && !replayName.isEmpty()) {
                  if (!modified.contains(replayName)) {
                     modified.add(replayName);
                  }
               } else if (!modified.contains(name)) {
                  modified.add(name);
               }
            } else if (!modified.contains(name)) {
               modified.add(name);
            }
         }

         for (String rName : ChatMorphHelper.getAllFilmReplayNames()) {
            if ((replayName == null || replayName.isEmpty() || morphName == null || !rName.equalsIgnoreCase(morphName)) && !modified.contains(rName)) {
               modified.add(rName);
            }
         }

         if (replayName != null && !replayName.isEmpty() && morphName != null && !morphName.equalsIgnoreCase(replayName)) {
            modified.removeIf(n -> n.equalsIgnoreCase(morphName));
         }

         cir.setReturnValue(modified);
      }
   }
}
