package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
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
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeGridAligner;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Recipe-book chrome, result icons, and ghost-recipe overlay for GUI clips. */
public final class GuiRecipeBookRenderer
{
    public static final int RECIPE_BOOK_WIDTH = 147;
    public static final int RECIPE_BOOK_HEIGHT = 166;
    public static final int RECIPE_OPEN_SHIFT = 77;

    private static final Identifier RECIPE_BUTTON = new Identifier("recipe_book/button");
    private static final Identifier RECIPE_BUTTON_HIGHLIGHTED = new Identifier("recipe_book/button_highlighted");
    private static final Identifier RECIPE_BOOK_TEXTURE = new Identifier("textures/gui/recipe_book.png");
    private static final Identifier RECIPE_FILTER_ON = new Identifier("recipe_book/filter_enabled");
    private static final Identifier RECIPE_FILTER_OFF = new Identifier("recipe_book/filter_disabled");
    private static final Identifier RECIPE_FILTER_ON_HOVER = new Identifier("recipe_book/filter_enabled_highlighted");
    private static final Identifier RECIPE_FILTER_OFF_HOVER = new Identifier("recipe_book/filter_disabled_highlighted");
    private static final Identifier RECIPE_TAB = new Identifier("recipe_book/tab");
    private static final Identifier RECIPE_TAB_SELECTED = new Identifier("recipe_book/tab_selected");
    private static final Identifier RECIPE_SLOT_CRAFTABLE = new Identifier("recipe_book/slot_craftable");
    private static final Identifier RECIPE_SLOT_UNCRAFTABLE = new Identifier("recipe_book/slot_uncraftable");
    private static final Identifier RECIPE_PAGE_FORWARD = new Identifier("recipe_book/page_forward");
    private static final Identifier RECIPE_PAGE_BACKWARD = new Identifier("recipe_book/page_backward");

    private static String cachedRecipeKey = null;
    private static List<RecipeIcon> cachedRecipeIcons = List.of();
    private static String cachedRecipeSearchQuery = null;
    private static Set<RecipeResultCollection> cachedRecipeSearchHits = null;

    private GuiRecipeBookRenderer()
    {
    }

    public static void drawRecipeButton(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick,
        int x,
        int y,
        float cursorGuiX,
        float cursorGuiY)
    {
        boolean hover = cursorGuiX >= x && cursorGuiX < x + 20 && cursorGuiY >= y && cursorGuiY < y + 18;
        KeyframeChannel<Boolean> recipeButton = clip.getRecipeButton(guiId);
        boolean recorded = GuiTextRenderer.sampleBool(recipeButton, tick, false);
        KeyframeChannel<Integer> mouseButtons = clip.getMouseButtons(guiId);
        int buttons = mouseButtons == null || mouseButtons.isEmpty() ? 0 : mouseButtons.interpolate(tick, 0);
        boolean clickOffButton = buttons != 0 && !hover;
        boolean highlighted = recipeButton == null || recipeButton.isEmpty()
            ? hover
            : hover || (recorded && !clickOffButton);
        batcher.getContext().drawGuiTexture(highlighted ? RECIPE_BUTTON_HIGHLIGHTED : RECIPE_BUTTON, x, y, 20, 18);
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
        GuiPointerHover hover)
    {
        int bookX = recipeBookX(originX, scaleX, screenWidth);
        int bookY = recipeBookY(originY, scaleY, screenHeight);
        String search = GuiTextRenderer.sampleString(clip.getRecipeSearch(guiId), tick, "");
        boolean showing = GuiTextRenderer.sampleBool(clip.getRecipeShowing(guiId), tick, false);
        int category = GuiTextRenderer.sampleInt(clip.getRecipeCategory(guiId), tick, 0);

        batcher.getContext().drawTexture(RECIPE_BOOK_TEXTURE, bookX, bookY, 1, 1, GuiRecipeBookRenderer.RECIPE_BOOK_WIDTH, GuiRecipeBookRenderer.RECIPE_BOOK_HEIGHT);

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
            GuiTextRenderer.sampleInt(clip.getRecipeSearchSelEnd(guiId), tick, search.length()));

        if (GuiRecipeBook.hasCategories(guiId))
        {
            List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
            int selected = MathHelper.clamp(category, 0, Math.max(0, groups.size() - 1));
            for (int i = 0; i < groups.size(); i++)
            {
                int tabX = bookX - 30;
                int tabY = bookY + 3 + i * 27;
                batcher.getContext().drawGuiTexture(
                    i == selected ? RECIPE_TAB_SELECTED : RECIPE_TAB,
                    tabX,
                    tabY,
                    35,
                    27);
            }
        }

        boolean filterHover = GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, bookX + 110, bookY + 12, 26, 16);
        Identifier filterTexture = showing
            ? (filterHover ? RECIPE_FILTER_ON_HOVER : RECIPE_FILTER_ON)
            : (filterHover ? RECIPE_FILTER_OFF_HOVER : RECIPE_FILTER_OFF);
        batcher.getContext().drawGuiTexture(filterTexture, bookX + 110, bookY + 12, 26, 16);
        if (filterHover)
        {
            hover.widget = recipeFilterTooltip(guiId, showing);
        }

        RecipeMatcher matcher = buildRecipeMatcher(clip, replayKeyframes, guiId, tick, globalTick);
        List<RecipeIcon> icons = listRecipeIcons(guiId, category, search, showing, matcher, tick);
        int pageCount = Math.max(1, MathHelper.ceilDiv(icons.size(), 20));
        int page = MathHelper.clamp(GuiTextRenderer.sampleInt(clip.getRecipePage(guiId), tick, 0), 0, pageCount - 1);
        int start = page * 20;
        int shown = Math.min(20, icons.size() - start);
        for (int i = 0; i < shown; i++)
        {
            int col = i % 5;
            int row = i / 5;
            int slotX = bookX + 11 + col * 25;
            int slotY = bookY + 31 + row * 25;
            RecipeIcon icon = icons.get(start + i);
            batcher.getContext().drawGuiTexture(
                icon.craftable ? RECIPE_SLOT_CRAFTABLE : RECIPE_SLOT_UNCRAFTABLE,
                slotX,
                slotY,
                25,
                25);
        }

        if (pageCount > 1)
        {
            String label = (page + 1) + "/" + pageCount;
            int labelWidth = MinecraftClient.getInstance().textRenderer.getWidth(label);
            int pageColor = ((int) (0xFF * opacity) << 24) | 0xFFFFFF;
            batcher.text(label, bookX + 73 - labelWidth / 2, bookY + 141, pageColor, false);
            if (page > 0)
            {
                batcher.getContext().drawGuiTexture(RECIPE_PAGE_BACKWARD, bookX + 38, bookY + 137, 12, 17);
            }
            if (page < pageCount - 1)
            {
                batcher.getContext().drawGuiTexture(RECIPE_PAGE_FORWARD, bookX + 93, bookY + 137, 12, 17);
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
        GuiPointerHover hover)
    {
        int bookX = recipeBookX(originX, scaleX, screenWidth);
        int bookY = recipeBookY(originY, scaleY, screenHeight);
        String search = GuiTextRenderer.sampleString(clip.getRecipeSearch(guiId), tick, "");
        boolean showing = GuiTextRenderer.sampleBool(clip.getRecipeShowing(guiId), tick, false);
        int category = GuiTextRenderer.sampleInt(clip.getRecipeCategory(guiId), tick, 0);

        DiffuseLighting.enableGuiDepthLighting();
        if (GuiRecipeBook.hasCategories(guiId))
        {
            List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
            for (int i = 0; i < groups.size(); i++)
            {
                List<ItemStack> tabIcons = groups.get(i).getIcons();
                if (tabIcons.isEmpty())
                {
                    continue;
                }
                int tabX = bookX - 30;
                int tabY = bookY + 3 + i * 27;
                batcher.getContext().getMatrices().push();
                batcher.getContext().getMatrices().translate(0F, 0F, 100F);
                if (tabIcons.size() == 1)
                {
                    batcher.getContext().drawItem(tabIcons.get(0), tabX + 9, tabY + 5);
                }
                else
                {
                    batcher.getContext().drawItem(tabIcons.get(0), tabX + 3, tabY + 5);
                    batcher.getContext().drawItem(tabIcons.get(1), tabX + 14, tabY + 5);
                }
                batcher.getContext().getMatrices().pop();
            }
        }

        RecipeMatcher matcher = buildRecipeMatcher(clip, replayKeyframes, guiId, tick, globalTick);
        List<RecipeIcon> icons = listRecipeIcons(guiId, category, search, showing, matcher, tick);
        int pageCount = Math.max(1, MathHelper.ceilDiv(icons.size(), 20));
        int page = MathHelper.clamp(GuiTextRenderer.sampleInt(clip.getRecipePage(guiId), tick, 0), 0, pageCount - 1);
        int start = page * 20;
        int shown = Math.min(20, icons.size() - start);
        for (int i = 0; i < shown; i++)
        {
            RecipeIcon icon = icons.get(start + i);
            if (icon.output.isEmpty())
            {
                continue;
            }
            int col = i % 5;
            int row = i / 5;
            int slotX = bookX + 11 + col * 25;
            int slotY = bookY + 31 + row * 25;
            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(0F, 0F, 100F);
            batcher.getContext().drawItem(icon.output, slotX + 4, slotY + 4);
            batcher.getContext().getMatrices().pop();
            if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, slotX, slotY, 25, 25))
            {
                hover.item = icon.output;
            }
        }
    }

    private static int recipeBookX(float originX, float scaleX, int screenWidth)
    {
        float bookScreenX = (screenWidth - GuiRecipeBookRenderer.RECIPE_BOOK_WIDTH * scaleX) / 2F - 86F * scaleX;
        return Math.round((bookScreenX - originX) / scaleX);
    }

    private static int recipeBookY(float originY, float scaleY, int screenHeight)
    {
        float bookScreenY = (screenHeight - GuiRecipeBookRenderer.RECIPE_BOOK_HEIGHT * scaleY) / 2F;
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
        float cursorGuiY)
    {
        String selectedId = GuiTextRenderer.sampleString(clip.getRecipeSelected(guiId), tick, "");
        if (selectedId == null || selectedId.isBlank())
        {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null)
        {
            return null;
        }

        Identifier recipeId = Identifier.tryParse(selectedId);
        RecipeEntry<?> entry = recipeId == null
            ? null
            : client.world.getRecipeManager().get(recipeId).orElse(null);
        if (entry == null)
        {
            return null;
        }

        Recipe<?> recipe = entry.value();
        DynamicRegistryManager registries = client.world.getRegistryManager();
        ItemStack result = recipe.getResult(registries);
        GuiSlotSchema schema = GuiSlotSchema.get(guiId);
        ItemStack[] hovered = {null};

        if (result != null && !result.isEmpty())
        {
            GuiSlotSchema.Slot resultSlot = schema.slots.stream()
                .filter(slot -> "craft_result".equals(slot.id()) || "result".equals(slot.id()))
                .findFirst()
                .orElse(null);
            if (resultSlot != null && GuiSlotRenderer.isSlotEmpty(clip, guiId, resultSlot.id(), tick))
            {
                drawRecipeGhostSlot(
                    batcher,
                    result,
                    resultSlot.x(),
                    resultSlot.y(),
                    "crafting_table".equals(guiId) || GuiRecipeBook.isFurnace(guiId),
                    true);
                if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, resultSlot.x(), resultSlot.y(), 16, 16))
                {
                    hovered[0] = result;
                }
            }
        }

        RecipeGridAligner<Ingredient> aligner = (inputs, handlerIndex, amount, gridX, gridY) ->
        {
            Ingredient ingredient = inputs.next();
            if (ingredient.isEmpty())
            {
                return;
            }
            GuiSlotSchema.Slot slot = schema.slotByHandlerIndex(handlerIndex);
            if (slot == null || !GuiSlotRenderer.isSlotEmpty(clip, guiId, slot.id(), tick))
            {
                return;
            }
            ItemStack[] stacks = ingredient.getMatchingStacks();
            if (stacks.length == 0)
            {
                return;
            }
            drawRecipeGhostSlot(batcher, stacks[0], slot.x(), slot.y(), false, false);
            if (GuiTextRenderer.inBounds(cursorGuiX, cursorGuiY, slot.x(), slot.y(), 16, 16))
            {
                hovered[0] = stacks[0];
            }
        };
        aligner.alignRecipeToGrid(
            GuiRecipeBook.gridWidth(guiId),
            GuiRecipeBook.gridHeight(guiId),
            GuiRecipeBook.resultSlotIndex(guiId),
            entry,
            recipe.getIngredients().iterator(),
            0);
        return hovered[0];
    }

    private static void drawRecipeGhostSlot(Batcher2D batcher, ItemStack stack, int x, int y, boolean wideResult, boolean result)
    {
        if (wideResult)
        {
            batcher.getContext().fill(x - 4, y - 4, x + 20, y + 20, 0x30FF0000);
        }
        else
        {
            batcher.getContext().fill(x, y, x + 16, y + 16, 0x30FF0000);
        }
        batcher.getContext().drawItemWithoutEntity(stack, x, y);
        batcher.getContext().fill(RenderLayer.getGuiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
        if (result)
        {
            batcher.getContext().drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);
        }
    }

    private static RecipeMatcher buildRecipeMatcher(
        GuiPovActionClip clip,
        ReplayKeyframes replayKeyframes,
        String guiId,
        float localTick,
        float globalTick)
    {
        RecipeMatcher matcher = new RecipeMatcher();
        GuiSlotSchema schema = GuiSlotSchema.get(guiId);
        RecordedHudData hudData = null;
        if (replayKeyframes instanceof ReplayKeyframesPovAccess access)
        {
            hudData = access.bbsPov$getHud();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (hudData == null && client.player != null)
        {
            client.player.getInventory().populateRecipeFinder(matcher);
            if (client.player.currentScreenHandler instanceof AbstractRecipeScreenHandler<?> recipeHandler)
            {
                recipeHandler.populateRecipeFinder(matcher);
            }
            return matcher;
        }

        if (schema.playerInventory && hudData != null)
        {
            for (int i = 0; i < Math.min(27, hudData.inventory.size()); i++)
            {
                matcher.addUnenchantedInput(sampleSlotStack(hudData.inventory.get(i), globalTick));
            }
        }
        if (replayKeyframes != null && replayKeyframes.hotbar != null)
        {
            for (int i = 0; i < Math.min(9, replayKeyframes.hotbar.size()); i++)
            {
                matcher.addUnenchantedInput(sampleSlotStack(replayKeyframes.hotbar.get(i), globalTick));
            }
        }
        for (GuiSlotSchema.Slot slot : schema.slots)
        {
            String id = slot.id();
            if ("craft_result".equals(id) || "result".equals(id) || id.startsWith("armor_") || "offhand".equals(id))
            {
                continue;
            }
            if (id.startsWith("craft_") || "input".equals(id) || "fuel".equals(id))
            {
                matcher.addInput(GuiSlotRenderer.sampleSlot(clip, guiId, id, localTick));
            }
        }
        return matcher;
    }

    private static ItemStack sampleSlotStack(KeyframeChannel<ItemStack> channel, float tick)
    {
        ItemStack stack = channel == null || channel.isEmpty()
            ? ItemStack.EMPTY
            : channel.interpolate(tick, ItemStack.EMPTY);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static List<RecipeIcon> listRecipeIcons(
        String guiId,
        int category,
        String search,
        boolean showing,
        RecipeMatcher matcher,
        float tick)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
        {
            return List.of();
        }

        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String cacheKey = guiId + ":" + category + ":" + query + ":" + showing + ":" + ((int) tick);
        if (cacheKey.equals(cachedRecipeKey))
        {
            return cachedRecipeIcons;
        }

        List<RecipeIcon> icons = new ArrayList<>();
        DynamicRegistryManager registries = client.world.getRegistryManager();
        List<RecipeBookGroup> groups = GuiRecipeBook.visibleGroups(guiId);
        RecipeBookGroup group = groups.isEmpty()
            ? RecipeBookGroup.CRAFTING_SEARCH
            : groups.get(MathHelper.clamp(category, 0, groups.size() - 1));

        ClientRecipeBook book = client.player.getRecipeBook();
        List<RecipeResultCollection> collections = book.getResultsForGroup(group);
        Set<RecipeResultCollection> searchHits = recipeSearchHits(client, query);
        int gridW = GuiRecipeBook.gridWidth(guiId);
        int gridH = GuiRecipeBook.gridHeight(guiId);

        for (RecipeResultCollection collection : collections)
        {
            collection.initialize(book);
            collection.computeCraftables(matcher, gridW, gridH, book);
            if (!collection.isInitialized() || !collection.hasFittingRecipes())
            {
                continue;
            }
            if (showing && !collection.hasCraftableRecipes())
            {
                continue;
            }
            if (searchHits != null && !searchHits.contains(collection))
            {
                continue;
            }
            List<RecipeEntry<?>> recipes = collection.getResults(false);
            if (recipes == null || recipes.isEmpty())
            {
                continue;
            }
            RecipeEntry<?> recipe = recipes.get(0);
            ItemStack output = recipe.value().getResult(registries);
            if (output == null || output.isEmpty())
            {
                continue;
            }
            icons.add(new RecipeIcon(output, collection.hasCraftableRecipes()));
        }

        cachedRecipeKey = cacheKey;
        cachedRecipeIcons = icons;
        return icons;
    }

    private static Set<RecipeResultCollection> recipeSearchHits(MinecraftClient client, String query)
    {
        if (query == null || query.isEmpty())
        {
            return null;
        }
        if (query.equals(cachedRecipeSearchQuery))
        {
            return cachedRecipeSearchHits;
        }
        try
        {
            SearchProvider<RecipeResultCollection> provider = client.getSearchProvider(SearchManager.RECIPE_OUTPUT);
            if (provider != null)
            {
                Set<RecipeResultCollection> hits = new HashSet<>(provider.findAll(query));
                cachedRecipeSearchQuery = query;
                cachedRecipeSearchHits = hits;
                return hits;
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    private static Text recipeFilterTooltip(String guiId, boolean showingCraftable)
    {
        if (!showingCraftable)
        {
            return Text.translatable("gui.recipebook.toggleRecipes.all");
        }
        String key = switch (guiId)
        {
            case "furnace" -> "gui.recipebook.toggleRecipes.smeltable";
            case "blast_furnace" -> "gui.recipebook.toggleRecipes.blastable";
            case "smoker" -> "gui.recipebook.toggleRecipes.smokable";
            default -> "gui.recipebook.toggleRecipes.craftable";
        };
        return Text.translatable(key);
    }




    private record RecipeIcon(ItemStack output, boolean craftable)
    {
    }
}
