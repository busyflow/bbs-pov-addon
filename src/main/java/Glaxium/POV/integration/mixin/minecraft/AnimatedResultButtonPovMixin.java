package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.recipebook.AnimatedResultButton;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AnimatedResultButton.class)
public abstract class AnimatedResultButtonPovMixin
{
    @Shadow
    protected abstract List<RecipeEntry<?>> getResults();

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void bbsPov$preventZeroDivideCrash(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        List<RecipeEntry<?>> results = this.getResults();
        if (results == null || results.isEmpty())
        {
            ((AnimatedResultButton) (Object) this).visible = false;
            info.cancel();
        }
    }

    @Inject(method = "currentRecipe", at = @At("HEAD"), cancellable = true)
    private void bbsPov$safeCurrentRecipe(CallbackInfoReturnable<RecipeEntry<?>> info)
    {
        List<RecipeEntry<?>> results = this.getResults();
        if (results == null || results.isEmpty())
        {
            info.setReturnValue(null);
        }
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void bbsPov$safeGetTooltip(CallbackInfoReturnable<List<Text>> info)
    {
        List<RecipeEntry<?>> results = this.getResults();
        if (results == null || results.isEmpty())
        {
            info.setReturnValue(List.of());
        }
    }

    @Inject(method = "appendClickableNarrations", at = @At("HEAD"), cancellable = true)
    private void bbsPov$safeAppendClickableNarrations(NarrationMessageBuilder builder, CallbackInfo info)
    {
        List<RecipeEntry<?>> results = this.getResults();
        if (results == null || results.isEmpty())
        {
            info.cancel();
        }
    }
}
