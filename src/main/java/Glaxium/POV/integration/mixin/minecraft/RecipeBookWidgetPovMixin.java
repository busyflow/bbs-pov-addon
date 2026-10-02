package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import Glaxium.POV.integration.access.minecraft.RecipeBookResultsPovAccess;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.gui.screen.recipebook.RecipeBookResults;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookWidgetPovMixin
{
    @Shadow @Final private RecipeBookResults recipesArea;
    @Shadow private boolean open;
    @Shadow private TextFieldWidget searchField;
    @Shadow private RecipeGroupButtonWidget currentTab;
    @Shadow @Final private List<RecipeGroupButtonWidget> tabButtons;
    @Shadow @Final protected RecipeBookGhostSlots ghostSlots;
    @Shadow private ClientRecipeBook recipeBook;
    @Shadow protected AbstractRecipeScreenHandler<?> craftingScreenHandler;

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$captureRecipeBook(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        int tab = 0;
        int visible = 0;
        if (this.tabButtons != null)
        {
            for (RecipeGroupButtonWidget button : this.tabButtons)
            {
                if (!button.visible)
                {
                    continue;
                }
                if (button == this.currentTab)
                {
                    tab = visible;
                    break;
                }
                visible++;
            }
        }

        Recipe<?> ghost = this.ghostSlots == null ? null : this.ghostSlots.getRecipe();
        boolean filtering = this.recipeBook != null && this.craftingScreenHandler != null
            && this.recipeBook.isFilteringCraftable(this.craftingScreenHandler);
        int selStart = 0;
        int selEnd = 0;
        if (this.searchField != null)
        {
            TextFieldWidgetPovAccessor access = (TextFieldWidgetPovAccessor) (Object) this.searchField;
            selStart = access.bbsPov$getSelectionStart();
            selEnd = access.bbsPov$getSelectionEnd();
        }

        GuiSnapshotCapture.updateRecipeBook(
            this.open,
            this.searchField == null ? "" : this.searchField.getText(),
            filtering,
            tab,
            ghost == null ? "" : ghost.getId().toString(),
            this.searchField != null && this.searchField.isFocused(),
            selStart,
            selEnd);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bbsPov$captureRecipePageAfterRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        this.bbsPov$captureRecipePage();
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void bbsPov$unfocusRecipeButton(
        double mouseX,
        double mouseY,
        int button,
        CallbackInfoReturnable<Boolean> info)
    {
        this.bbsPov$captureRecipePage();
        if (Boolean.TRUE.equals(info.getReturnValue()))
        {
            GuiSnapshotCapture.updateRecipeButton(false);
        }
    }

    @Unique
    private void bbsPov$captureRecipePage()
    {
        if (this.recipesArea instanceof RecipeBookResultsPovAccess access)
        {
            GuiSnapshotCapture.updateRecipePage(access.bbsPov$getCurrentPage());
        }
    }
}
