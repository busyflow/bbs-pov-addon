package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Stonecutter clip renderer. Saved type id remains {@code stonecutter}. */
public final class StonecutterGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final StonecutterGuiRenderer INSTANCE = new StonecutterGuiRenderer();

    private static final Identifier STONECUTTER_SCROLLER = new Identifier("container/stonecutter/scroller");
    private static final Identifier STONECUTTER_SCROLLER_DISABLED = new Identifier("container/stonecutter/scroller_disabled");
    private static final Identifier STONECUTTER_RECIPE = new Identifier("container/stonecutter/recipe");
    private static final Identifier STONECUTTER_RECIPE_SELECTED = new Identifier("container/stonecutter/recipe_selected");
    private static final Identifier STONECUTTER_RECIPE_HOVER = new Identifier("container/stonecutter/recipe_highlighted");

    private StonecutterGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawEarlyChrome(GuiRenderContext ctx)
    {
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        drawChrome(ctx.batcher, ctx.clip, ctx.localTick, cursorX, cursorY, ctx.hover);
    }

    @Override
    public void drawLateItems(GuiRenderContext ctx)
    {
        drawRecipeItems(ctx.batcher, ctx.clip, ctx.localTick);
    }

    private static void drawChrome(
        Batcher2D batcher,
        GuiPovActionClip clip,
        float tick,
        float cursorX,
        float cursorY,
        GuiPointerHover hover)
    {
        List<ItemStack> recipes = listStonecutterRecipes(GuiSlotRenderer.sampleSlot(clip, "stonecutter", "input", tick));
        ItemStack output = GuiSlotRenderer.sampleSlot(clip, "stonecutter", "result", tick);
        int selected = findStonecutterSelection(recipes, output);
        int topRow = stonecutterTopRow(clip, tick, recipes.size(), selected);
        int maxTopRow = Math.max(0, MathHelper.ceilDiv(recipes.size(), 4) - 3);
        boolean canScroll = recipes.size() > 12;
        int scrollY = maxTopRow == 0 ? 0 : Math.round(topRow * 41F / maxTopRow);
        batcher.getContext().drawGuiTexture(
            canScroll ? STONECUTTER_SCROLLER : STONECUTTER_SCROLLER_DISABLED,
            119,
            15 + scrollY,
            12,
            15);

        int first = topRow * 4;
        int shown = Math.min(12, recipes.size() - first);
        for (int i = 0; i < shown; i++)
        {
            int index = first + i;
            int x = 52 + (i % 4) * 16;
            int y = 14 + (i / 4) * 18 + 2;
            boolean hovered = GuiTextRenderer.inBounds(cursorX, cursorY, x, y - 1, 16, 18);
            Identifier texture = index == selected
                ? STONECUTTER_RECIPE_SELECTED
                : hovered ? STONECUTTER_RECIPE_HOVER : STONECUTTER_RECIPE;
            batcher.getContext().drawGuiTexture(texture, x, y - 1, 16, 18);
            if (hovered)
            {
                hover.item = recipes.get(index);
            }
        }
    }

    private static void drawRecipeItems(Batcher2D batcher, GuiPovActionClip clip, float tick)
    {
        List<ItemStack> recipes = listStonecutterRecipes(GuiSlotRenderer.sampleSlot(clip, "stonecutter", "input", tick));
        ItemStack output = GuiSlotRenderer.sampleSlot(clip, "stonecutter", "result", tick);
        int selected = findStonecutterSelection(recipes, output);
        int topRow = stonecutterTopRow(clip, tick, recipes.size(), selected);
        int first = topRow * 4;
        int shown = Math.min(12, recipes.size() - first);
        DiffuseLighting.enableGuiDepthLighting();
        for (int i = 0; i < shown; i++)
        {
            int x = 52 + (i % 4) * 16;
            int y = 14 + (i / 4) * 18 + 2;
            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(0F, 0F, 100F);
            batcher.getContext().drawItem(recipes.get(first + i), x, y);
            batcher.getContext().getMatrices().pop();
        }
    }

    private static List<ItemStack> listStonecutterRecipes(ItemStack input)
    {
        List<ItemStack> recipes = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || input == null || input.isEmpty())
        {
            return recipes;
        }
        DynamicRegistryManager registries = client.world.getRegistryManager();
        SimpleInventory inventory = new SimpleInventory(input.copy());
        for (RecipeEntry<StonecuttingRecipe> entry : client.world.getRecipeManager()
            .getAllMatches(RecipeType.STONECUTTING, inventory, client.world))
        {
            ItemStack result = entry.value().getResult(registries);
            if (result != null && !result.isEmpty())
            {
                recipes.add(result.copy());
            }
        }
        return recipes;
    }

    private static int findStonecutterSelection(List<ItemStack> recipes, ItemStack output)
    {
        if (output == null || output.isEmpty())
        {
            return -1;
        }
        for (int i = 0; i < recipes.size(); i++)
        {
            if (ItemStack.areItemsEqual(recipes.get(i), output))
            {
                return i;
            }
        }
        return -1;
    }

    private static int stonecutterTopRow(GuiPovActionClip clip, float tick, int recipeCount, int selected)
    {
        int maxTopRow = Math.max(0, MathHelper.ceilDiv(recipeCount, 4) - 3);
        int fallback = selected < 0 ? 0 : MathHelper.clamp(selected / 4, 0, maxTopRow);
        if (clip.stonecutterRow == null || clip.stonecutterRow.isEmpty())
        {
            return fallback;
        }
        return MathHelper.clamp(clip.stonecutterRow.interpolate(tick, fallback), 0, maxTopRow);
    }

}
