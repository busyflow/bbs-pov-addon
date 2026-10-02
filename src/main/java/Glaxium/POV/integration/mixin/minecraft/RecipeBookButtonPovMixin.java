package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TexturedButtonWidget.class)
public abstract class RecipeBookButtonPovMixin
{
    @Shadow @Final protected ButtonTextures textures;

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void bbsPov$captureRecipeButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        if (this.textures == RecipeBookWidget.BUTTON_TEXTURES)
        {
            GuiSnapshotCapture.updateRecipeButton(((TexturedButtonWidget) (Object) this).isSelected());
        }
    }
}
