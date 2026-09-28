package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.fabricmc.fabric.impl.client.itemgroup.CreativeGuiExtensions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreativeInventoryScreen.class})
public abstract class CreativeInventoryScreenPovMixin {
   @Shadow
   private static ItemGroup selectedTab;
   @Shadow
   private float scrollPosition;
   @Shadow
   private TextFieldWidget searchBox;

   @Inject(
      method = {"handledScreenTick"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureCreativeState(CallbackInfo info) {
      TextFieldWidget box = this.searchBox;
      int selStart = 0;
      int selEnd = 0;
      if (box != null) {
         TextFieldWidgetPovAccessor access = (TextFieldWidgetPovAccessor)box;
         selStart = access.bbsPov$getSelectionStart();
         selEnd = access.bbsPov$getSelectionEnd();
      }

      GuiSnapshotCapture.updateCreative(
         selectedTab,
         this.scrollPosition,
         box == null ? "" : box.getText(),
         ((CreativeGuiExtensions)this).fabric_currentPage(),
         box != null && box.isVisible() && box.isFocused(),
         selStart,
         selEnd
      );
   }

   @Inject(
      method = {"init"},
      at = {@At("RETURN")}
   )
   private void bbsPov$captureOnInit(CallbackInfo info) {
      this.bbsPov$captureCreativeState(info);
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureOnRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
      this.bbsPov$captureCreativeState(info);
   }

   @Inject(
      method = {"removed"},
      at = {@At("HEAD")}
   )
   private void bbsPov$resetOnClose(CallbackInfo info) {
      GuiSnapshotCapture.resetCreative();
   }
}
