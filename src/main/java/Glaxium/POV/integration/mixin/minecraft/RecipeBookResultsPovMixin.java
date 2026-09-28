package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import Glaxium.POV.integration.access.minecraft.RecipeBookResultsPovAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RecipeBookResults.class})
public abstract class RecipeBookResultsPovMixin implements RecipeBookResultsPovAccess {
   @Shadow
   private int currentPage;

   @Override
   public int bbsPov$getCurrentPage() {
      return this.currentPage;
   }

   @Inject(
      method = {"draw"},
      at = {@At("HEAD")}
   )
   private void bbsPov$captureRecipePage(DrawContext context, int x, int y, int mouseX, int mouseY, float delta, CallbackInfo info) {
      GuiSnapshotCapture.updateRecipePage(this.currentPage);
   }
}
