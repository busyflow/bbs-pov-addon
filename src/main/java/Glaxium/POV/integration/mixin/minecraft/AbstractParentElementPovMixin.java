package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractParentElement.class)
public abstract class AbstractParentElementPovMixin
{
    @Inject(method = "setFocused(Lnet/minecraft/client/gui/Element;)V", at = @At("RETURN"))
    private void bbsPov$clearRecipeButtonFocus(Element focused, CallbackInfo info)
    {
        if (focused instanceof TexturedButtonWidget button
            && ((TexturedButtonWidgetPovAccessor) button).bbsPov$getTextures() == RecipeBookWidget.BUTTON_TEXTURES)
        {
            return;
        }

        GuiSnapshotCapture.updateRecipeButton(false);
    }
}
