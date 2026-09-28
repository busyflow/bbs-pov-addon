package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.playback.PovPlaybackInput;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Films;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public abstract class MinecraftClientPovInputMixin {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void bbsPov$detachLiveUseState(CallbackInfo info) {
      MinecraftClient client = (MinecraftClient)(Object)this;
      if (PovPlaybackContext.getActive() != null || PovPlaybackInput.isLocked()) {
         if (client.currentScreen instanceof HandledScreen
            || client.currentScreen instanceof BookScreen
            || client.currentScreen instanceof BookEditScreen
            || client.currentScreen instanceof GameModeSelectionScreen) {
            client.setScreen(null);
         }

         ClientPlayerEntity player = client.player;
         client.options.useKey.setPressed(false);
         client.options.attackKey.setPressed(false);
         if (player != null) {
            ((ClientPlayerEntityPovAccessor)player).bbsPov$setUsingItem(false);
            ((ClientPlayerEntityPovAccessor)player).bbsPov$setClientActiveHand(Hand.MAIN_HAND);
            ((LivingEntityPovAccessor)player).bbsPov$setActiveItemStack(ItemStack.EMPTY);
            ((LivingEntityPovAccessor)player).bbsPov$setItemUseTimeLeft(0);
         }
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void bbsPov$keepRecordingOpenGuis(CallbackInfo info) {
      MinecraftClient client = (MinecraftClient)(Object)this;
      if (client.isPaused()) {
         Screen screen = client.currentScreen;
         if (screen instanceof BookScreen
            || screen instanceof BookEditScreen
            || screen instanceof HandledScreen
            || screen instanceof GameModeSelectionScreen
            || screen instanceof GameMenuScreen
            || screen instanceof SleepingChatScreen) {
            Films films = BBSModClient.getFilms();
            if (films != null && films.getRecorder() != null) {
               films.update();
            }
         }
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$blockAttack(CallbackInfoReturnable<Boolean> info) {
      if (PovPlaybackInput.isLocked() || PovPlaybackContext.getActive() != null) {
         info.setReturnValue(false);
      }
   }

   @Inject(
      method = {"doItemUse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$blockUse(CallbackInfo info) {
      if (PovPlaybackInput.isLocked() || PovPlaybackContext.getActive() != null) {
         info.cancel();
      }
   }
}
