package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class GuiRecipeBookRenderer {
   public static final int RECIPE_BOOK_WIDTH = 147;
   public static final int RECIPE_BOOK_HEIGHT = 166;
   public static final int RECIPE_OPEN_SHIFT = 77;
   private static final Identifier RECIPE_BUTTON_TEXTURE = new Identifier("textures/gui/recipe_button.png");
   private static final Identifier RECIPE_BOOK_TEXTURE = new Identifier("textures/gui/recipe_book.png");
   private static String cachedRecipeKey = null;
   private static List<GuiRecipeBookRenderer.RecipeIcon> cachedRecipeIcons = List.of();
   private static String cachedRecipeSearchQuery = null;
   private static Set<RecipeResultCollection> cachedRecipeSearchHits = null;

   private GuiRecipeBookRenderer() {
   }

   public static void drawRecipeButton(Batcher2D batcher, GuiPovActionClip clip, String guiId, float tick, int x, int y, float cursorGuiX, float cursorGuiY) {
      boolean hover = cursorGuiX >= (float)x && cursorGuiX < (float)(x + 20) && cursorGuiY >= (float)y && cursorGuiY < (float)(y + 18);
      KeyframeChannel<Boolean> recipeButton = clip.getRecipeButton(guiId);
      boolean recorded = GuiTextRenderer.sampleBool(recipeButton, tick, false);
      KeyframeChannel<Integer> mouseButtons = clip.getMouseButtons(guiId);
      int buttons = mouseButtons != null && !mouseButtons.isEmpty() ? (Integer)mouseButtons.interpolate(tick, 0) : 0;
      boolean clickOffButton = buttons != 0 && !hover;
      boolean highlighted = recipeButton != null && !recipeButton.isEmpty() ? hover || recorded && !clickOffButton : hover;
      batcher.getContext().drawTexture(RECIPE_BUTTON_TEXTURE, x, y, 0, highlighted ? 19 : 0, 20, 18);
   }

   public static void render(
      Batcher2D batcher,
      GuiPovActionClip clip,
      ReplayKeyframes replayKeyframes,
      String guiId,
      float tick,
      float globalTick,
      float originX,
      float originY,
      float scaleX,
      float scaleY,
      int screenWidth,
      int screenHeight,
      float opacity,
      float cursorGuiX,
      float cursorGuiY,
      GuiPointerHover hover
   ) {
      int bookX = recipeBookX(originX, scaleX, screenWidth);
      int bookY = recipeBookY(originY, scaleY, screenHeight);
      String search = GuiTextRenderer.sampleString(clip.getRecipeSearch(guiId), tick, "");
      boolean showing = GuiTextRenderer.sampleBool(clip.getRecipeShowing(guiId), tick, false);
      int category = GuiTextRenderer.sampleInt(clip.getRecipeCategory(guiId), tick, 0);
      batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, bookX, bookY, 1, 1, 147, 166);
      boolean searchHover = GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, bookX + 25, bookY + 13, 81, 14);
      boolean searchFocused = GuiTextRenderer.isSearchFocused(clip.getRecipeSearchFocus(guiId), tick, search, searchHover);
      GuiTextRenderer.drawSearchField(
         batcher,
         search,
         bookX + 25,
         bookY + 13,
         81,
         14,
         searchFocused,
         true,
         opacity,
         GuiTextRenderer.sampleInt(clip.getRecipeSearchSelStart(guiId), tick, search.length()),
         GuiTextRenderer.sampleInt(clip.getRecipeSearchSelEnd(guiId), tick, search.length())
      );
      if (GuiRecipeBook.hasCategories(guiId)) {
         List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
         int selected = MathHelper.clamp(category, 0, Math.max(0, groups.size() - 1));

         for (int i = 0; i < groups.size(); i++) {
            int tabX = bookX - 30;
            int tabY = bookY + 3 + i * 27;
            batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, tabX - (i == selected ? 2 : 0), tabY, i == selected ? 188 : 153, 2, 35, 27);
         }
      }

      boolean filterHover = GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, bookX + 110, bookY + 12, 26, 16);

      int filterV = switch (guiId) {
         case "furnace", "blast_furnace", "smoker" -> 182;
         default -> 41;
      };
      batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, bookX + 110, bookY + 12, showing ? 180 : 152, filterV + (filterHover ? 18 : 0), 26, 16);
      if (filterHover) {
         hover.widget = recipeFilterTooltip(guiId, showing);
      }

      RecipeMatcher matcher = buildRecipeMatcher(clip, replayKeyframes, guiId, tick, globalTick);
      List<GuiRecipeBookRenderer.RecipeIcon> icons = listRecipeIcons(guiId, category, search, showing, matcher, tick);
      int pageCount = Math.max(1, MathHelper.ceilDiv(icons.size(), 20));
      int page = MathHelper.clamp(GuiTextRenderer.sampleInt(clip.getRecipePage(guiId), tick, 0), 0, pageCount - 1);
      int start = page * 20;
      int shown = Math.min(20, icons.size() - start);

      for (int i = 0; i < shown; i++) {
         int col = i % 5;
         int row = i / 5;
         int slotX = bookX + 11 + col * 25;
         int slotY = bookY + 31 + row * 25;
         GuiRecipeBookRenderer.RecipeIcon icon = icons.get(start + i);
         batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, slotX, slotY, icon.craftable ? 29 : 54, 206, 25, 25);
      }

      if (pageCount > 1) {
         String label = page + 1 + "/" + pageCount;
         int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(label);
         int pageColor = (int)(255.0F * opacity) << 24 | 16777215;
         batcher.text(label, (float)(bookX + 73 - labelWidth / 2), (float)(bookY + 141), pageColor, false);
         if (page > 0) {
            batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, bookX + 38, bookY + 137, 14, 208, 12, 17);
         }

         if (page < pageCount - 1) {
            batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, bookX + 93, bookY + 137, 1, 208, 12, 17);
         }
      }
   }

   public static void renderItems(
      Batcher2D batcher,
      GuiPovActionClip clip,
      ReplayKeyframes replayKeyframes,
      String guiId,
      float tick,
      float globalTick,
      float originX,
      float originY,
      float scaleX,
      float scaleY,
      int screenWidth,
      int screenHeight,
      float cursorGuiX,
      float cursorGuiY,
      GuiPointerHover hover
   ) {
      int bookX = recipeBookX(originX, scaleX, screenWidth);
      int bookY = recipeBookY(originY, scaleY, screenHeight);
      String search = GuiTextRenderer.sampleString(clip.getRecipeSearch(guiId), tick, "");
      boolean showing = GuiTextRenderer.sampleBool(clip.getRecipeShowing(guiId), tick, false);
      int category = GuiTextRenderer.sampleInt(clip.getRecipeCategory(guiId), tick, 0);
      DiffuseLighting.enableGuiDepthLighting();
      if (GuiRecipeBook.hasCategories(guiId)) {
         List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
         int selected = MathHelper.clamp(category, 0, Math.max(0, groups.size() - 1));

         for (int i = 0; i < groups.size(); i++) {
            List<ItemStack> tabIcons = groups.get(i).getIcons();
            if (!tabIcons.isEmpty()) {
               int tabX = bookX - 30 - (i == selected ? 2 : 0);
               int tabY = bookY + 3 + i * 27;
               batcher.getContext().getMatrices().push();
               batcher.getContext().getMatrices().translate(0.0F, 0.0F, 100.0F);
               if (tabIcons.size() == 1) {
                  batcher.getContext().drawItem(tabIcons.get(0), tabX + 9, tabY + 5);
               } else {
                  batcher.getContext().drawItem(tabIcons.get(0), tabX + 3, tabY + 5);
                  batcher.getContext().drawItem(tabIcons.get(1), tabX + 14, tabY + 5);
               }

               batcher.getContext().getMatrices().pop();
            }
         }
      }

      RecipeMatcher matcher = buildRecipeMatcher(clip, replayKeyframes, guiId, tick, globalTick);
      List<GuiRecipeBookRenderer.RecipeIcon> icons = listRecipeIcons(guiId, category, search, showing, matcher, tick);
      int pageCount = Math.max(1, MathHelper.ceilDiv(icons.size(), 20));
      int page = MathHelper.clamp(GuiTextRenderer.sampleInt(clip.getRecipePage(guiId), tick, 0), 0, pageCount - 1);
      int start = page * 20;
      int shown = Math.min(20, icons.size() - start);

      for (int ix = 0; ix < shown; ix++) {
         GuiRecipeBookRenderer.RecipeIcon icon = icons.get(start + ix);
         if (!icon.output.isEmpty()) {
            int col = ix % 5;
            int row = ix / 5;
            int slotX = bookX + 11 + col * 25;
            int slotY = bookY + 31 + row * 25;
            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(0.0F, 0.0F, 100.0F);
            batcher.getContext().drawItem(icon.output, slotX + 4, slotY + 4);
            batcher.getContext().getMatrices().pop();
            if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, slotX, slotY, 25, 25)) {
               hover.item = icon.output;
            }
         }
      }
   }

   private static int recipeBookX(float originX, float scaleX, int screenWidth) {
      float bookScreenX = ((float)screenWidth - 147.0F * scaleX) / 2.0F - 86.0F * scaleX;
      return Math.round((bookScreenX - originX) / scaleX);
   }

   private static int recipeBookY(float originY, float scaleY, int screenHeight) {
      float bookScreenY = ((float)screenHeight - 166.0F * scaleY) / 2.0F;
      return Math.round((bookScreenY - originY) / scaleY);
   }

   public static ItemStack renderGhosts(
      Batcher2D batcher,
      GuiPovActionClip clip,
      ReplayKeyframes replayKeyframes,
      String guiId,
      float tick,
      float globalTick,
      float opacity,
      float cursorGuiX,
      float cursorGuiY
   ) {
      String selectedId = GuiTextRenderer.sampleString(clip.getRecipeSelected(guiId), tick, "");
      if (selectedId != null && !selectedId.isBlank()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.world == null) {
            return null;
         } else {
            Identifier recipeId = Identifier.tryParse(selectedId);
            Recipe<?> recipe = recipeId == null ? null : (Recipe)client.world.getRecipeManager().get(recipeId).orElse(null);
            if (recipe == null) {
               return null;
            } else {
               DynamicRegistryManager registries = client.world.getRegistryManager();
               ItemStack result = recipe.getOutput(registries);
               GuiSlotSchema schema = GuiSlotSchema.get(guiId);
               ItemStack[] hovered = new ItemStack[]{null};
               if (result != null && !result.isEmpty()) {
                  GuiSlotSchema.Slot resultSlot = schema.slots
                     .stream()
                     .filter(slot -> "craft_result".equals(slot.id()) || "result".equals(slot.id()))
                     .findFirst()
                     .orElse(null);
                  if (resultSlot != null && GuiSlotRenderer.isSlotEmpty(clip, guiId, resultSlot.id(), tick)) {
                     drawRecipeGhostSlot(
                        batcher, result, resultSlot.x(), resultSlot.y(), "crafting_table".equals(guiId) || GuiRecipeBook.isFurnace(guiId), true
                     );
                     if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, resultSlot.x(), resultSlot.y(), 16, 16)) {
                        hovered[0] = result;
                     }
                  }
               }

               RecipeGridAligner<Ingredient> aligner = (inputs, handlerIndex, amount, gridX, gridY) -> {
                  Ingredient ingredient = (Ingredient)inputs.next();
                  if (!ingredient.isEmpty()) {
                     GuiSlotSchema.Slot slot = schema.slotByHandlerIndex(handlerIndex);
                     if (slot != null && GuiSlotRenderer.isSlotEmpty(clip, guiId, slot.id(), tick)) {
                        ItemStack[] stacks = ingredient.getMatchingStacks();
                        if (stacks.length != 0) {
                           drawRecipeGhostSlot(batcher, stacks[0], slot.x(), slot.y(), false, false);
                           if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, slot.x(), slot.y(), 16, 16)) {
                              hovered[0] = stacks[0];
                           }
                        }
                     }
                  }
               };
               aligner.alignRecipeToGrid(
                  GuiRecipeBook.gridWidth(guiId),
                  GuiRecipeBook.gridHeight(guiId),
                  GuiRecipeBook.resultSlotIndex(guiId),
                  recipe,
                  recipe.getIngredients().iterator(),
                  0
               );
               return hovered[0];
            }
         }
      } else {
         return null;
      }
   }

   private static void drawRecipeGhostSlot(Batcher2D batcher, ItemStack stack, int x, int y, boolean wideResult, boolean result) {
      if (wideResult) {
         batcher.getContext().fill(x - 4, y - 4, x + 20, y + 20, 822018048);
      } else {
         batcher.getContext().fill(x, y, x + 16, y + 16, 822018048);
      }

      batcher.getContext().drawItemWithoutEntity(stack, x, y);
      batcher.getContext().fill(RenderLayer.getGuiGhostRecipeOverlay(), x, y, x + 16, y + 16, 822083583);
      if (result) {
         batcher.getContext().drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);
      }
   }

   private static RecipeMatcher buildRecipeMatcher(GuiPovActionClip clip, ReplayKeyframes replayKeyframes, String guiId, float localTick, float globalTick) {
      RecipeMatcher matcher = new RecipeMatcher();
      GuiSlotSchema schema = GuiSlotSchema.get(guiId);
      RecordedHudData hudData = null;
      if (replayKeyframes instanceof ReplayKeyframesPovAccess access) {
         hudData = access.bbsPov$getHud();
      }

      MinecraftClient client = MinecraftClient.getInstance();
      if (hudData == null && client.player != null) {
         client.player.getInventory().populateRecipeFinder(matcher);
         if (client.player.currentScreenHandler instanceof AbstractRecipeScreenHandler<?> recipeHandler) {
            recipeHandler.populateRecipeFinder(matcher);
         }

         return matcher;
      } else {
         if (schema.playerInventory && hudData != null) {
            for (int i = 0; i < Math.min(27, hudData.inventory.size()); i++) {
               matcher.addUnenchantedInput(sampleSlotStack(hudData.inventory.get(i), globalTick));
            }
         }

         if (replayKeyframes != null && replayKeyframes.hotbar != null) {
            for (int i = 0; i < Math.min(9, replayKeyframes.hotbar.size()); i++) {
               matcher.addUnenchantedInput(sampleSlotStack((KeyframeChannel<ItemStack>)replayKeyframes.hotbar.get(i), globalTick));
            }
         }

         for (GuiSlotSchema.Slot slot : schema.slots) {
            String id = slot.id();
            if (!"craft_result".equals(id)
               && !"result".equals(id)
               && !id.startsWith("armor_")
               && !"offhand".equals(id)
               && (id.startsWith("craft_") || "input".equals(id) || "fuel".equals(id))) {
               matcher.addInput(GuiSlotRenderer.sampleSlot(clip, guiId, id, localTick));
            }
         }

         return matcher;
      }
   }

   private static ItemStack sampleSlotStack(KeyframeChannel<ItemStack> channel, float tick) {
      ItemStack stack = channel != null && !channel.isEmpty() ? (ItemStack)channel.interpolate(tick, ItemStack.EMPTY) : ItemStack.EMPTY;
      return stack == null ? ItemStack.EMPTY : stack;
   }

   private static List<GuiRecipeBookRenderer.RecipeIcon> listRecipeIcons(
      String guiId, int category, String search, boolean showing, RecipeMatcher matcher, float tick
   ) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null && client.player != null) {
         String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
         String cacheKey = guiId + ":" + category + ":" + query + ":" + showing + ":" + (int)tick;
         if (cacheKey.equals(cachedRecipeKey)) {
            return cachedRecipeIcons;
         } else {
            List<GuiRecipeBookRenderer.RecipeIcon> icons = new ArrayList<>();
            DynamicRegistryManager registries = client.world.getRegistryManager();
            List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
            RecipeBookGroup group = groups.isEmpty() ? RecipeBookGroup.CRAFTING_SEARCH : groups.get(MathHelper.clamp(category, 0, groups.size() - 1));
            ClientRecipeBook book = client.player.getRecipeBook();
            List<RecipeResultCollection> collections = book.getResultsForGroup(group);
            Set<RecipeResultCollection> searchHits = recipeSearchHits(client, query);
            int gridW = GuiRecipeBook.gridWidth(guiId);
            int gridH = GuiRecipeBook.gridHeight(guiId);

            for (RecipeResultCollection collection : collections) {
               collection.initialize(book);
               collection.computeCraftables(matcher, gridW, gridH, book);
               if (collection.isInitialized()
                  && collection.hasFittingRecipes()
                  && (!showing || collection.hasCraftableRecipes())
                  && (searchHits == null || searchHits.contains(collection))) {
                  List<Recipe<?>> recipes = collection.getResults(false);
                  if (recipes != null && !recipes.isEmpty()) {
                     Recipe<?> recipe = recipes.get(0);
                     ItemStack output = recipe.getOutput(registries);
                     if (output != null && !output.isEmpty()) {
                        icons.add(new GuiRecipeBookRenderer.RecipeIcon(output, collection.hasCraftableRecipes()));
                     }
                  }
               }
            }

            cachedRecipeKey = cacheKey;
            cachedRecipeIcons = icons;
            return icons;
         }
      } else {
         return List.of();
      }
   }

   private static Set<RecipeResultCollection> recipeSearchHits(MinecraftClient client, String query) {
      if (query == null || query.isEmpty()) {
         return null;
      } else if (query.equals(cachedRecipeSearchQuery)) {
         return cachedRecipeSearchHits;
      } else {
         try {
            SearchProvider<RecipeResultCollection> provider = client.getSearchProvider(SearchManager.RECIPE_OUTPUT);
            if (provider != null) {
               Set<RecipeResultCollection> hits = new HashSet<>(provider.findAll(query));
               cachedRecipeSearchQuery = query;
               cachedRecipeSearchHits = hits;
               return hits;
            }
         } catch (Exception var4) {
         }

         return null;
      }
   }

   private static Text recipeFilterTooltip(String guiId, boolean showingCraftable) {
      if (!showingCraftable) {
         return Text.translatable("gui.recipebook.toggleRecipes.all");
      } else {
         String key = switch (guiId) {
            case "furnace" -> "gui.recipebook.toggleRecipes.smeltable";
            case "blast_furnace" -> "gui.recipebook.toggleRecipes.blastable";
            case "smoker" -> "gui.recipebook.toggleRecipes.smokable";
            default -> "gui.recipebook.toggleRecipes.craftable";
         };
         return Text.translatable(key);
      }
   }

   private static record RecipeIcon(ItemStack output, boolean craftable) {
   }
}
