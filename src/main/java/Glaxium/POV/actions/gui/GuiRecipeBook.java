package Glaxium.POV.actions.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Which GUI types own a vanilla recipe book, and how that book is keyed. */
public final class GuiRecipeBook
{
    private static final Set<String> ALL = Set.of(
        "inventory",
        "crafting_table",
        "furnace",
        "blast_furnace",
        "smoker");

    private GuiRecipeBook()
    {
    }

    public static boolean supports(String guiId)
    {
        return ALL.contains(guiId);
    }

    public static boolean isFurnace(String guiId)
    {
        return "furnace".equals(guiId) || "blast_furnace".equals(guiId) || "smoker".equals(guiId);
    }

    public static boolean hasCategories(String guiId)
    {
        return groups(guiId).size() > 1;
    }

    public static RecipeBookCategory category(String guiId)
    {
        return switch (guiId)
        {
            case "furnace" -> RecipeBookCategory.FURNACE;
            case "blast_furnace" -> RecipeBookCategory.BLAST_FURNACE;
            case "smoker" -> RecipeBookCategory.SMOKER;
            default -> RecipeBookCategory.CRAFTING;
        };
    }

    public static RecipeType<?> recipeType(String guiId)
    {
        return switch (guiId)
        {
            case "furnace" -> RecipeType.SMELTING;
            case "blast_furnace" -> RecipeType.BLASTING;
            case "smoker" -> RecipeType.SMOKING;
            default -> RecipeType.CRAFTING;
        };
    }

    public static List<RecipeBookGroup> groups(String guiId)
    {
        return RecipeBookGroup.getGroups(category(guiId));
    }

    /**
     * Tabs vanilla actually shows: search is always present, other categories only
     * if the player's recipe book has known recipes for them.
     */
    public static List<RecipeBookGroup> visibleGroups(String guiId)
    {
        List<RecipeBookGroup> all = groups(guiId);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
        {
            return all;
        }

        ClientRecipeBook book = client.player.getRecipeBook();
        List<RecipeBookGroup> visible = new ArrayList<>();
        for (RecipeBookGroup group : all)
        {
            if (isSearch(group) || hasKnownRecipes(book, group))
            {
                visible.add(group);
            }
        }
        return visible.isEmpty() ? all : visible;
    }

    public static boolean isSearch(RecipeBookGroup group)
    {
        return group == RecipeBookGroup.CRAFTING_SEARCH
            || group == RecipeBookGroup.FURNACE_SEARCH
            || group == RecipeBookGroup.BLAST_FURNACE_SEARCH
            || group == RecipeBookGroup.SMOKER_SEARCH;
    }

    private static boolean hasKnownRecipes(ClientRecipeBook book, RecipeBookGroup group)
    {
        List<RecipeResultCollection> results = book.getResultsForGroup(group);
        if (results == null || results.isEmpty())
        {
            return false;
        }
        for (RecipeResultCollection collection : results)
        {
            if (!collection.isInitialized())
            {
                collection.initialize(book);
            }
            if (collection.hasFittingRecipes())
            {
                return true;
            }
        }
        return false;
    }

    public static int gridWidth(String guiId)
    {
        return switch (guiId)
        {
            case "inventory" -> 2;
            case "furnace", "blast_furnace", "smoker" -> 1;
            default -> 3;
        };
    }

    public static int gridHeight(String guiId)
    {
        return gridWidth(guiId);
    }

    /** Screen-handler index of the result slot, matching vanilla recipe book alignment. */
    public static int resultSlotIndex(String guiId)
    {
        return switch (guiId)
        {
            case "furnace", "blast_furnace", "smoker" -> 2;
            default -> 0;
        };
    }
}
