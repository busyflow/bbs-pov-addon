package Glaxium.POV.actions.gui.schema;

import Glaxium.POV.integration.access.minecraft.HorseScreenPovAccess;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.BlastFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
import net.minecraft.client.gui.screen.ingame.CrafterScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.LoomScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.client.gui.screen.ingame.SmokerScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.screen.GenericContainerScreenHandler;

/**
 * Single screen→saved-id mapping. Never serialize id {@code mule}; mules use {@code donkey}.
 * {@link #resolve} returns null for unknown screens. Recording of unknown handled screens
 * uses {@link #resolveOrInventory}.
 */
public final class GuiTypeResolver
{
    private GuiTypeResolver()
    {
    }

    public static String resolve(Screen screen)
    {
        if (screen instanceof BookEditScreen || screen instanceof BookScreen)
        {
            return "book";
        }
        if (screen instanceof GameModeSelectionScreen)
        {
            return "gamemode_switcher";
        }
        if (!(screen instanceof HandledScreen<?> handled))
        {
            return null;
        }
        return resolveHandled(handled);
    }

    public static String resolveOrInventory(HandledScreen<?> handled)
    {
        String id = resolveHandled(handled);
        return id == null ? "inventory" : id;
    }

    private static String resolveHandled(HandledScreen<?> handled)
    {
        if (handled instanceof CreativeInventoryScreen) return "creative_inventory";
        if (handled instanceof InventoryScreen) return "inventory";
        if (handled instanceof CraftingScreen) return "crafting_table";
        if (handled instanceof AnvilScreen) return "anvil";
        if (handled instanceof ShulkerBoxScreen) return "shulker_box";
        if (handled instanceof Generic3x3ContainerScreen)
        {
            String title = handled.getTitle().getString().toLowerCase(java.util.Locale.ROOT);
            return title.contains("dropper") ? "dropper" : "dispenser";
        }
        if (handled instanceof CrafterScreen) return "crafter";
        if (handled instanceof GenericContainerScreen containerScreen)
        {
            String title = handled.getTitle().getString().toLowerCase(java.util.Locale.ROOT);
            if (title.contains("barrel")) return "barrel";
            if (title.contains("ender")) return "ender_chest";
            if (containerScreen.getScreenHandler() instanceof GenericContainerScreenHandler)
            {
                GenericContainerScreenHandler handler = (GenericContainerScreenHandler) containerScreen.getScreenHandler();
                if (handler.getRows() > 3)
                {
                    return "large_chest";
                }
            }
            return "chest";
        }
        if (handled instanceof FurnaceScreen) return "furnace";
        if (handled instanceof BlastFurnaceScreen) return "blast_furnace";
        if (handled instanceof SmokerScreen) return "smoker";
        if (handled instanceof EnchantmentScreen) return "enchanting_table";
        if (handled instanceof BrewingStandScreen) return "brewing_stand";
        if (handled instanceof SmithingScreen) return "smithing_table";
        if (handled instanceof GrindstoneScreen) return "grindstone";
        if (handled instanceof StonecutterScreen) return "stonecutter";
        if (handled instanceof CartographyTableScreen) return "cartography_table";
        if (handled instanceof LoomScreen) return "loom";
        if (handled instanceof HopperScreen) return "hopper";
        if (handled instanceof MerchantScreen) return "villager";
        if (handled instanceof HorseScreen)
        {
            return resolveMount(handled instanceof HorseScreenPovAccess horseScreen
                ? horseScreen.bbsPov$getEntity()
                : null);
        }
        if (handled instanceof BeaconScreen) return "beacon";
        return null;
    }

    /**
     * Saved mount GUI id. Donkeys and mules both return {@code donkey}.
     * Never returns {@code mule}.
     */
    public static String resolveMount(AbstractHorseEntity mount)
    {
        if (mount instanceof DonkeyEntity || mount instanceof MuleEntity)
        {
            return "donkey";
        }
        return "horse";
    }
}
