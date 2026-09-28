package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.LoomScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LoomScreen.class})
public abstract class LoomScreenPovMixin {
   @Shadow
   private int visibleTopRow;

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureLoomState(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      GuiSnapshotCapture.updateLoom(this.visibleTopRow);
   }

   @Inject(
      method = {"mouseScrolled"},
      at = {@At("RETURN")}
   )
   private void bbsPov$captureLoomWheel(double mouseX, double mouseY, double amount, CallbackInfoReturnable<Boolean> info) {
      GuiSnapshotCapture.updateLoom(this.visibleTopRow);
   }

   @Inject(
      method = {"mouseDragged"},
      at = {@At("RETURN")}
   )
   private void bbsPov$captureLoomScrollbar(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> info) {
      GuiSnapshotCapture.updateLoom(this.visibleTopRow);
   }
}
