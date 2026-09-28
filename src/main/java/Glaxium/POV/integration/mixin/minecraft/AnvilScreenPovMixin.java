package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AnvilScreen.class})
public abstract class AnvilScreenPovMixin {
   @Shadow
   private TextFieldWidget nameField;

   @Inject(
      method = {"renderForeground"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureAnvil(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      AnvilScreen screen = (AnvilScreen)(Object)this;
      AnvilScreenHandler handler = (AnvilScreenHandler)screen.getScreenHandler();
      Slot input = handler.getSlot(0);
      Slot addition = handler.getSlot(1);
      Slot result = handler.getSlot(handler.getResultSlotIndex());
      boolean error = (input.hasStack() || addition.hasStack()) && !result.hasStack();
      int selStart = 0;
      int selEnd = 0;
      if (this.nameField != null) {
         TextFieldWidgetPovAccessor access = (TextFieldWidgetPovAccessor)(Object)this.nameField;
         selStart = access.bbsPov$getSelectionStart();
         selEnd = access.bbsPov$getSelectionEnd();
      }

      GuiSnapshotCapture.updateAnvil(
         this.nameField == null ? "" : this.nameField.getText(), this.nameField != null && this.nameField.isFocused(), selStart, selEnd, error
      );
   }
}
