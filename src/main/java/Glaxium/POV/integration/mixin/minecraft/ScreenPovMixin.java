package Glaxium.POV.integration.mixin.minecraft;

import mchorse.bbs_mod.BBSModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Screen.class})
public abstract class ScreenPovMixin {
   @Inject(
      method = {"renderWithTooltip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$suppressScreenDuringLiveVideoOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      MinecraftClient client = MinecraftClient.getInstance();
      boolean videoRecording = BBSModClient.getVideoRecorder() != null && BBSModClient.getVideoRecorder().isRecording();
      if (videoRecording && (client.currentScreen != null || client.player != null && client.player.getSleepTimer() > 0)) {
         info.cancel();
      }
   }
}
