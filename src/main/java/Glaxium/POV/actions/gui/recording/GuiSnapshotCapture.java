package Glaxium.POV.actions.gui.recording;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.CreativeInventoryTabs;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.data.AnvilSnapshot;
import Glaxium.POV.actions.gui.data.BeaconSnapshot;
import Glaxium.POV.actions.gui.data.BookSnapshot;
import Glaxium.POV.actions.gui.data.BrewingSnapshot;
import Glaxium.POV.actions.gui.data.CreativeSnapshot;
import Glaxium.POV.actions.gui.data.EnchantmentSnapshot;
import Glaxium.POV.actions.gui.data.FurnaceSnapshot;
import Glaxium.POV.actions.gui.data.GamemodeSnapshot;
import Glaxium.POV.actions.gui.data.GuiCapture;
import Glaxium.POV.actions.gui.data.GuiSnapshot;
import Glaxium.POV.actions.gui.data.LoomSnapshot;
import Glaxium.POV.actions.gui.data.MerchantSnapshot;
import Glaxium.POV.actions.gui.data.MountSnapshot;
import Glaxium.POV.actions.gui.data.RecipeBookSnapshot;
import Glaxium.POV.actions.gui.data.StonecutterSnapshot;
import Glaxium.POV.actions.gui.schema.GuiTypeResolver;
import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.integration.access.minecraft.BeaconScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.BookEditScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.BookScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.GameModeSelectionScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.HandledScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.HorseScreenPovAccess;
import Glaxium.POV.integration.access.minecraft.MerchantScreenPovAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared screen read path for recording and live preview. */
public final class GuiSnapshotCapture
{
    private static AnvilSnapshot anvilCache;
    private static CreativeSnapshot creativeCache;
    private static LoomSnapshot loomCache;
    private static StonecutterSnapshot stonecutterCache;
    private static EnchantmentSnapshot enchantmentCache;
    private static RecipeBookSnapshot recipeBookCache = new RecipeBookSnapshot(
        false, "", false, 0, "", 0, false, false, 0, 0);

    private GuiSnapshotCapture()
    {
    }

    public static void updateAnvil(String name, boolean focused, int selStart, int selEnd, boolean error)
    {
        anvilCache = new AnvilSnapshot(name, focused, selStart, selEnd, error);
    }

    public static void updateCreative(
        ItemGroup selected,
        float scrollPosition,
        String searchText,
        int currentPage,
        boolean focused,
        int selStart,
        int selEnd)
    {
        List<ItemGroup> groups = CreativeInventoryTabs.groups();
        int index = groups.indexOf(selected);
        int tab = index < 0 ? 0 : index;
        float scroll = Math.max(0F, Math.min(1F, scrollPosition));
        String search = searchText == null ? "" : searchText;
        int page = Math.max(0, currentPage);
        boolean inventoryTab = selected != null && selected.getType() == ItemGroup.Type.INVENTORY;
        int itemCount = selected == null ? 0 : (selected.getType() == ItemGroup.Type.SEARCH
            ? selected.getSearchTabStacks().size()
            : selected.getDisplayStacks().size());
        int rows = Math.max(5, (itemCount + 8) / 9);
        int row = Math.round(scroll * Math.max(0, rows - 5));
        creativeCache = new CreativeSnapshot(
            tab, scroll, search, page, inventoryTab, row, focused, selStart, selEnd);
    }

    public static void resetCreative()
    {
        creativeCache = null;
    }

    public static void resetScreenCaches()
    {
        creativeCache = null;
        anvilCache = null;
        loomCache = null;
        stonecutterCache = null;
        enchantmentCache = null;
    }

    public static void updateLoom(int visibleTopRow)
    {
        loomCache = new LoomSnapshot(visibleTopRow);
    }

    public static void updateStonecutter(int scrollOffset)
    {
        stonecutterCache = new StonecutterSnapshot(Math.max(0, scrollOffset / 4));
    }

    public static void updateEnchantment(
        int[] enchantPower,
        int[] enchantId,
        int[] enchantLevel,
        int tableSeed,
        int experienceLevel,
        boolean creativeMode,
        float turningSpeed)
    {
        enchantmentCache = new EnchantmentSnapshot(
            EnchantmentSnapshot.pack(enchantPower, enchantId, enchantLevel),
            tableSeed,
            experienceLevel,
            creativeMode,
            turningSpeed);
    }

    public static void updateRecipeBook(
        boolean bookOpen,
        String searchText,
        boolean craftableOnly,
        int tab,
        String selectedRecipe,
        int currentPage,
        boolean focused,
        int selStart,
        int selEnd)
    {
        RecipeBookSnapshot previous = recipeBookCache;
        recipeBookCache = new RecipeBookSnapshot(
            bookOpen,
            searchText,
            craftableOnly,
            tab,
            selectedRecipe,
            currentPage,
            previous == null ? false : previous.buttonSelected,
            focused,
            selStart,
            selEnd);
    }

    public static void updateRecipeBook(
        boolean bookOpen,
        String searchText,
        boolean craftableOnly,
        int tab,
        String selectedRecipe,
        boolean focused,
        int selStart,
        int selEnd)
    {
        updateRecipeBook(
            bookOpen,
            searchText,
            craftableOnly,
            tab,
            selectedRecipe,
            recipeBookCache == null ? 0 : recipeBookCache.page,
            focused,
            selStart,
            selEnd);
    }

    public static void updateRecipePage(int currentPage)
    {
        RecipeBookSnapshot previous = recipeBookCache;
        if (previous == null)
        {
            recipeBookCache = new RecipeBookSnapshot(
                false, "", false, 0, "", Math.max(0, currentPage), false, false, 0, 0);
            return;
        }
        recipeBookCache = new RecipeBookSnapshot(
            previous.open,
            previous.search,
            previous.showing,
            previous.category,
            previous.selected,
            currentPage,
            previous.buttonSelected,
            previous.searchFocused,
            previous.searchSelStart,
            previous.searchSelEnd);
    }

    public static void updateRecipeButton(boolean selected)
    {
        RecipeBookSnapshot previous = recipeBookCache;
        if (previous == null)
        {
            recipeBookCache = new RecipeBookSnapshot(
                false, "", false, 0, "", 0, selected, false, 0, 0);
            return;
        }
        recipeBookCache = new RecipeBookSnapshot(
            previous.open,
            previous.search,
            previous.showing,
            previous.category,
            previous.selected,
            previous.page,
            selected,
            previous.searchFocused,
            previous.searchSelStart,
            previous.searchSelEnd);
    }

    public static GuiCapture capture(Screen screen, int screenWidth, int screenHeight)
    {
        String guiType = GuiTypeResolver.resolve(screen);
        if (guiType == null)
        {
            return null;
        }
        return captureTyped(screen, guiType, screenWidth, screenHeight);
    }

    public static GuiCapture captureOrInventory(HandledScreen<?> handled, int screenWidth, int screenHeight)
    {
        String guiType = GuiTypeResolver.resolveOrInventory(handled);
        return captureTyped(handled, guiType, screenWidth, screenHeight);
    }

    private static GuiCapture captureTyped(Screen screen, String guiType, int screenWidth, int screenHeight)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        double mouseX = client.mouse.getX() * (double) screenWidth / (double) client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * (double) screenHeight / (double) client.getWindow().getHeight();
        float curTx = (float) ((mouseX - (screenWidth / 2.0)) / HotbarLayoutTransform.PIXELS_PER_UNIT);
        float curTy = (float) (((screenHeight / 2.0) - mouseY) / HotbarLayoutTransform.PIXELS_PER_UNIT);

        Map<String, ItemStack> slots = new LinkedHashMap<>();
        ItemStack cursorItem = ItemStack.EMPTY;
        boolean dragging = false;
        String dragEncoded = "";
        List<String> dragKeys = List.of();

        if (screen instanceof HandledScreen<?> handled)
        {
            ScreenHandler handler = handled.getScreenHandler();
            if ("creative_inventory".equals(guiType)
                && creativeCache != null
                && creativeCache.inventoryTab
                && client.player != null)
            {
                handler = client.player.playerScreenHandler;
            }
            if (handler != null)
            {
                GuiSlotSchema schema = GuiSlotSchema.get(guiType);
                if (schema != null && schema.slots != null)
                {
                    for (GuiSlotSchema.Slot slot : schema.slots)
                    {
                        if (slot.handlerIndex() >= 0 && slot.handlerIndex() < handler.slots.size())
                        {
                            ItemStack stack = handler.slots.get(slot.handlerIndex()).getStack();
                            slots.put(slot.id(), stack == null ? ItemStack.EMPTY : stack.copy());
                        }
                    }
                }
                ItemStack cursorStack = handler.getCursorStack();
                cursorItem = cursorStack == null ? ItemStack.EMPTY : cursorStack.copy();
                HandledScreenPovAccess dragAccess = handled instanceof HandledScreenPovAccess screenDrag
                    ? screenDrag
                    : null;
                dragging = dragAccess != null
                    && dragAccess.bbsPov$isCursorDragging()
                    && dragAccess.bbsPov$getCursorDragSlots() != null
                    && dragAccess.bbsPov$getCursorDragSlots().size() > 1;
                if (dragging)
                {
                    cursorItem = cursorItem.isEmpty()
                        ? ItemStack.EMPTY
                        : cursorItem.copyWithCount(Math.max(0, dragAccess.bbsPov$getDraggedStackRemainder()));
                    dragKeys = GuiSlotDragPreview.keys(dragAccess.bbsPov$getCursorDragSlots(), guiType);
                    dragEncoded = GuiSlotDragPreview.encode(
                        dragAccess.bbsPov$getHeldButtonType(),
                        cursorStack == null ? 0 : cursorStack.getCount(),
                        cursorStack,
                        dragKeys);
                }
            }
        }

        GuiSnapshot snapshot = new GuiSnapshot(
            guiType, curTx, curTy, true, cursorItem, dragging, dragEncoded, slots, dragKeys);
        return new GuiCapture(
            snapshot,
            "anvil".equals(guiType) ? anvilCache : null,
            "creative_inventory".equals(guiType) ? creativeCache : null,
            "loom".equals(guiType) ? loomCache : null,
            "stonecutter".equals(guiType) ? stonecutterCache : null,
            "enchanting_table".equals(guiType) ? enchantmentCache : null,
            captureBeacon(screen, guiType),
            GuiRecipeBook.supports(guiType) ? captureRecipeBook(screen, guiType) : null,
            captureMerchant(screen, guiType),
            captureBook(screen, guiType),
            captureMount(screen, guiType),
            captureGamemode(screen, guiType),
            captureFurnace(screen, guiType),
            captureBrewing(screen, guiType));
    }

    /**
     * Prefer live ClientRecipeBook / RecipeBookWidget state for open + craftable
     * filter. The render mixin cache can still be the default false/false on the
     * first recording tick before the widget has rendered.
     */
    private static RecipeBookSnapshot captureRecipeBook(Screen screen, String guiType)
    {
        RecipeBookSnapshot cached = recipeBookCache == null
            ? new RecipeBookSnapshot(false, "", false, 0, "", 0, false, false, 0, 0)
            : recipeBookCache;

        boolean open = cached.open;
        boolean showing = cached.showing;
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null)
        {
            ClientRecipeBook book = client.player.getRecipeBook();
            RecipeBookCategory category = GuiRecipeBook.category(guiType);
            open = book.isGuiOpen(category);
            showing = book.isFilteringCraftable(category);

            if (screen instanceof HandledScreen<?> handled
                && handled.getScreenHandler() instanceof AbstractRecipeScreenHandler<?> recipeHandler)
            {
                showing = book.isFilteringCraftable(recipeHandler);
            }
        }

        if (screen instanceof RecipeBookProvider provider)
        {
            RecipeBookWidget widget = provider.getRecipeBookWidget();

            if (widget != null)
            {
                open = widget.isOpen();
            }
        }

        RecipeBookSnapshot live = new RecipeBookSnapshot(
            open,
            cached.search,
            showing,
            cached.category,
            cached.selected,
            cached.page,
            cached.buttonSelected,
            cached.searchFocused,
            cached.searchSelStart,
            cached.searchSelEnd);
        recipeBookCache = live;
        return live;
    }

    private static BeaconSnapshot captureBeacon(Screen screen, String guiType)
    {
        if (!"beacon".equals(guiType) || !(screen instanceof HandledScreen<?> handled)
            || !(handled.getScreenHandler() instanceof BeaconScreenHandler beacon))
        {
            return null;
        }
        StatusEffect primary = handled instanceof BeaconScreenPovAccess accessor
            ? accessor.bbsPov$getPrimaryEffect() : beacon.getPrimaryEffect();
        StatusEffect secondary = handled instanceof BeaconScreenPovAccess accessor
            ? accessor.bbsPov$getSecondaryEffect() : beacon.getSecondaryEffect();
        int secondaryIndex = secondary == StatusEffects.REGENERATION
            ? 1 : secondary != null && secondary == primary ? 2 : 0;
        return new BeaconSnapshot(beaconPrimaryIndex(primary), secondaryIndex, beacon.getProperties());
    }

    private static MerchantSnapshot captureMerchant(Screen screen, String guiType)
    {
        if (!"villager".equals(guiType) || !(screen instanceof MerchantScreen merchantScreen))
        {
            return null;
        }
        var merchantHandler = merchantScreen.getScreenHandler();
        int scrollIndex = merchantScreen instanceof MerchantScreenPovAccess acc
            ? acc.bbsPov$getIndexStartOffset()
            : 0;
        int selectedIndex = merchantScreen instanceof MerchantScreenPovAccess acc
            ? acc.bbsPov$getSelectedIndex()
            : 0;
        TradeOfferList recipes = merchantHandler.getRecipes();
        int level = merchantHandler.getLevelProgress();
        int xp = merchantHandler.getExperience();
        boolean canLevel = merchantHandler.isLeveled();
        String title = merchantScreen.getTitle() != null ? merchantScreen.getTitle().getString() : "";

        int profession = 1;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.targetedEntity instanceof net.minecraft.entity.passive.VillagerEntity villager)
        {
            profession = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getRawId(
                villager.getVillagerData().getProfession());
        }
        return MerchantSnapshot.fromOffers(
            recipes, profession, level, xp, selectedIndex, scrollIndex, title, canLevel);
    }

    private static BookSnapshot captureBook(Screen screen, String guiType)
    {
        if (!"book".equals(guiType))
        {
            return null;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        String author = player != null ? player.getName().getString() : "";

        if (screen instanceof BookEditScreenPovAccess edit)
        {
            boolean signing = edit.bbsPov$isSigning();
            SelectionManager selection = signing ? edit.bbsPov$getTitleSelection() : edit.bbsPov$getPageSelection();
            return new BookSnapshot(
                true,
                signing,
                edit.bbsPov$getCurrentPage(),
                BookSnapshot.pack(edit.bbsPov$getPages()),
                edit.bbsPov$getTitle(),
                author,
                selection == null ? 0 : selection.getSelectionStart(),
                selection == null ? 0 : selection.getSelectionEnd());
        }

        if (screen instanceof BookScreenPovAccess book)
        {
            BookScreen.Contents contents = book.bbsPov$getContents();
            List<String> pages = new ArrayList<>();
            int count = contents == null ? 0 : contents.getPageCount();
            for (int i = 0; i < count; i++)
            {
                StringVisitable visitable = contents.getPage(i);
                if (visitable instanceof Text text)
                {
                    pages.add(Text.Serializer.toJson(text));
                }
                else
                {
                    pages.add(visitable == null ? "" : visitable.getString());
                }
            }
            return new BookSnapshot(
                false, false, book.bbsPov$getPageIndex(), BookSnapshot.pack(pages), "", author, 0, 0);
        }
        return null;
    }

    private static MountSnapshot captureMount(Screen screen, String guiType)
    {
        if (!("horse".equals(guiType) || "donkey".equals(guiType))
            || !(screen instanceof HandledScreen<?> handled))
        {
            return null;
        }
        net.minecraft.entity.passive.AbstractHorseEntity mount = handled instanceof HorseScreenPovAccess horseScreen
            ? horseScreen.bbsPov$getEntity()
            : null;
        int variant = 0;
        boolean chest = false;
        if ("horse".equals(guiType) && mount instanceof net.minecraft.entity.passive.HorseEntity horse)
        {
            variant = GuiPovActionClip.unpackHorseVariant(horse.getVariant().getId(), horse.getMarking().getId());
        }
        if ("donkey".equals(guiType) && mount instanceof net.minecraft.entity.passive.AbstractDonkeyEntity donkey)
        {
            chest = donkey.hasChest();
        }
        return new MountSnapshot(variant, chest);
    }

    private static GamemodeSnapshot captureGamemode(Screen screen, String guiType)
    {
        if (!"gamemode_switcher".equals(guiType))
        {
            return null;
        }
        int selected = 0;
        if (screen instanceof GameModeSelectionScreenPovAccess acc)
        {
            Object modeObj = acc.bbsPov$getGameMode();
            if (modeObj != null)
            {
                String modeName = modeObj.toString();
                if ("CREATIVE".equals(modeName)) selected = 1;
                else if ("ADVENTURE".equals(modeName)) selected = 2;
                else if ("SPECTATOR".equals(modeName)) selected = 3;
            }
        }
        else if (MinecraftClient.getInstance().interactionManager != null)
        {
            GameMode gm = MinecraftClient.getInstance().interactionManager.getCurrentGameMode();
            if (gm == GameMode.CREATIVE) selected = 1;
            else if (gm == GameMode.ADVENTURE) selected = 2;
            else if (gm == GameMode.SPECTATOR) selected = 3;
        }
        return new GamemodeSnapshot(selected);
    }

    private static FurnaceSnapshot captureFurnace(Screen screen, String guiType)
    {
        if (!GuiRecipeBook.isFurnace(guiType)
            || !(screen instanceof HandledScreen<?> handled)
            || !(handled.getScreenHandler() instanceof AbstractFurnaceScreenHandler furnace))
        {
            return null;
        }
        return new FurnaceSnapshot(
            furnace.isBurning() ? furnace.getFuelProgress() : 0F,
            furnace.getCookProgress());
    }

    private static BrewingSnapshot captureBrewing(Screen screen, String guiType)
    {
        if (!"brewing_stand".equals(guiType)
            || !(screen instanceof HandledScreen<?> handled)
            || !(handled.getScreenHandler() instanceof BrewingStandScreenHandler brewing))
        {
            return null;
        }
        int brewTime = brewing.getBrewTime();
        return new BrewingSnapshot(
            brewTime > 0 ? 1F - brewTime / 400F : 0F,
            brewing.getFuel() / 20F,
            brewTime > 0);
    }

    private static int beaconPrimaryIndex(StatusEffect effect)
    {
        if (effect == StatusEffects.SPEED) return 1;
        if (effect == StatusEffects.HASTE) return 2;
        if (effect == StatusEffects.RESISTANCE) return 3;
        if (effect == StatusEffects.JUMP_BOOST) return 4;
        if (effect == StatusEffects.STRENGTH) return 5;
        return 0;
    }
}
