package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class StonecutterGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final StonecutterGuiRenderer INSTANCE = new StonecutterGuiRenderer();
   private static final Identifier STONECUTTER_TEXTURE = new Identifier("textures/gui/container/stonecutter.png");

   private StonecutterGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawEarlyChrome(GuiRenderContext ctx) {
      float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000.0F;
      float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000.0F;
      drawChrome(ctx.batcher, ctx.clip, ctx.localTick, cursorX, cursorY, ctx.hover);
   }

   @Override
   public void drawLateItems(GuiRenderContext ctx) {
      drawRecipeItems(ctx.batcher, ctx.clip, ctx.localTick);
   }

   private static void drawChrome(Batcher2D batcher, GuiPovActionClip clip, float tick, float cursorX, float cursorY, GuiPointerHover hover) {
      List<ItemStack> recipes = listStonecutterRecipes(GuiSlotRenderer.sampleSlot(clip, "stonecutter", "input", tick));
      ItemStack output = GuiSlotRenderer.sampleSlot(clip, "stonecutter", "result", tick);
      int selected = findStonecutterSelection(recipes, output);
      int topRow = stonecutterTopRow(clip, tick, recipes.size(), selected);
      int maxTopRow = Math.max(0, MathHelper.ceilDiv(recipes.size(), 4) - 3);
      boolean canScroll = recipes.size() > 12;
      int scrollY = maxTopRow == 0 ? 0 : Math.round((float)topRow * 41.0F / (float)maxTopRow);
      batcher.getContext().drawTexture(STONECUTTER_TEXTURE, 119, 15 + scrollY, 176 + (canScroll ? 0 : 12), 0, 12, 15);
      int first = topRow * 4;
      int shown = Math.min(12, recipes.size() - first);

      for (int i = 0; i < shown; i++) {
         int index = first + i;
         int x = 52 + i % 4 * 16;
         int y = 14 + i / 4 * 18 + 2;
         boolean hovered = GuiTextRenderer.inBounds(cursorX, cursorY, x, y - 1, 16, 18);
         int v = index == selected ? 184 : (hovered ? 202 : 166);
         batcher.getContext().drawTexture(STONECUTTER_TEXTURE, x, y - 1, 0, v, 16, 18);
         if (hovered) {
            hover.item = recipes.get(index);
         }
      }
   }

   private static void drawRecipeItems(Batcher2D batcher, GuiPovActionClip clip, float tick) {
      List<ItemStack> recipes = listStonecutterRecipes(GuiSlotRenderer.sampleSlot(clip, "stonecutter", "input", tick));
      ItemStack output = GuiSlotRenderer.sampleSlot(clip, "stonecutter", "result", tick);
      int selected = findStonecutterSelection(recipes, output);
      int topRow = stonecutterTopRow(clip, tick, recipes.size(), selected);
      int first = topRow * 4;
      int shown = Math.min(12, recipes.size() - first);
      DiffuseLighting.enableGuiDepthLighting();

      for (int i = 0; i < shown; i++) {
         int x = 52 + i % 4 * 16;
         int y = 14 + i / 4 * 18 + 2;
         batcher.getContext().getMatrices().push();
         batcher.getContext().getMatrices().translate(0.0F, 0.0F, 100.0F);
         batcher.getContext().drawItem(recipes.get(first + i), x, y);
         batcher.getContext().getMatrices().pop();
      }
   }

   private static List<ItemStack> listStonecutterRecipes(ItemStack input) {
      List<ItemStack> recipes = new ArrayList<>();
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && input != null && !input.isEmpty()) {
         DynamicRegistryManager registries = client.world.getRegistryManager();
         SimpleInventory inventory = new SimpleInventory(new ItemStack[]{input.copy()});

         for (StonecuttingRecipe entry : client.world.getRecipeManager().getAllMatches(RecipeType.STONECUTTING, inventory, client.world)) {
            ItemStack result = entry.getOutput(registries);
            if (result != null && !result.isEmpty()) {
               recipes.add(result.copy());
            }
         }

         return recipes;
      } else {
         return recipes;
      }
   }

   private static int findStonecutterSelection(List<ItemStack> recipes, ItemStack output) {
      if (output != null && !output.isEmpty()) {
         for (int i = 0; i < recipes.size(); i++) {
            if (ItemStack.areItemsEqual(recipes.get(i), output)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static int stonecutterTopRow(GuiPovActionClip clip, float tick, int recipeCount, int selected) {
      int maxTopRow = Math.max(0, MathHelper.ceilDiv(recipeCount, 4) - 3);
      int fallback = selected < 0 ? 0 : MathHelper.clamp(selected / 4, 0, maxTopRow);
      return clip.stonecutterRow != null && !clip.stonecutterRow.isEmpty()
         ? MathHelper.clamp((Integer)clip.stonecutterRow.interpolate(tick, fallback), 0, maxTopRow)
         : fallback;
   }
}
