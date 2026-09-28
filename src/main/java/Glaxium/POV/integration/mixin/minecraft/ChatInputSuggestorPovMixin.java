package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.chat.ChatMorphHelper;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatInputSuggestor.class})
public class ChatInputSuggestorPovMixin {
   @Shadow
   @Final
   MinecraftClient client;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;

   @Inject(
      method = {"showCommandSuggestions"},
      at = {@At("HEAD")}
   )
   private void bbsPov$filterAndReplacePlayerName(CallbackInfo ci) {
      if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
         try {
            Suggestions original = this.pendingSuggestions.getNow(null);
            if (original != null && !original.isEmpty()) {
               String playerName = this.client.player != null && this.client.player.getGameProfile() != null
                  ? this.client.player.getGameProfile().getName()
                  : null;
               String morphName = this.client.player != null ? ChatMorphHelper.getPlayerMorphName(this.client.player) : null;
               String replayName = ChatMorphHelper.getActiveReplayName();
               List<String> filmReplays = ChatMorphHelper.getAllFilmReplayNames();
               if (morphName != null && !morphName.isEmpty() || replayName != null && !replayName.isEmpty() || !filmReplays.isEmpty()) {
                  List<Suggestion> modified = new ArrayList<>();
                  List<String> addedTexts = new ArrayList<>();
                  String primaryName = replayName != null && !replayName.isEmpty()
                     ? replayName
                     : (morphName != null && !morphName.isEmpty() ? morphName : (!filmReplays.isEmpty() ? filmReplays.get(0) : null));

                  for (Suggestion s : original.getList()) {
                     if (playerName != null && s.getText().equalsIgnoreCase(playerName)) {
                        if (primaryName != null && !addedTexts.contains(primaryName)) {
                           modified.add(new Suggestion(s.getRange(), primaryName, s.getTooltip()));
                           addedTexts.add(primaryName);
                        }
                     } else if (replayName != null && !replayName.isEmpty() && morphName != null && s.getText().equalsIgnoreCase(morphName)) {
                        if (!addedTexts.contains(replayName)) {
                           modified.add(new Suggestion(s.getRange(), replayName, s.getTooltip()));
                           addedTexts.add(replayName);
                        }
                     } else if (!addedTexts.contains(s.getText())) {
                        modified.add(s);
                        addedTexts.add(s.getText());
                     }
                  }

                  if (replayName != null && !replayName.isEmpty() && morphName != null && !morphName.equalsIgnoreCase(replayName)) {
                     modified.removeIf(sx -> sx.getText().equalsIgnoreCase(morphName));
                  }

                  this.pendingSuggestions = CompletableFuture.completedFuture(new Suggestions(original.getRange(), modified));
               }
            }
         } catch (Throwable var12) {
         }
      }
   }
}
