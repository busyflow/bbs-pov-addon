package Glaxium.POV.actions.gui.schema;

import Glaxium.POV.integration.access.minecraft.HorseScreenPovAccess;
import java.util.Locale;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.client.gui.screen.ingame.BlastFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.BrewingStandScreen;
import net.minecraft.client.gui.screen.ingame.CartographyTableScreen;
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

public final class GuiTypeResolver {
   private GuiTypeResolver() {
   }

   public static String resolve(Screen screen) {
      if (screen instanceof BookEditScreen || screen instanceof BookScreen) {
         return "book";
      } else if (screen instanceof GameModeSelectionScreen) {
         return "gamemode_switcher";
      } else {
         return screen instanceof HandledScreen<?> handled ? resolveHandled(handled) : null;
      }
   }

   public static String resolveOrInventory(HandledScreen<?> handled) {
      String id = resolveHandled(handled);
      return id == null ? "inventory" : id;
   }

   private static String resolveHandled(HandledScreen<?> handled) {
      if (handled instanceof CreativeInventoryScreen) {
         return "creative_inventory";
      } else if (handled instanceof InventoryScreen) {
         return "inventory";
      } else if (handled instanceof CraftingScreen) {
         return "crafting_table";
      } else if (handled instanceof AnvilScreen) {
         return "anvil";
      } else if (handled instanceof ShulkerBoxScreen) {
         return "shulker_box";
      } else if (handled instanceof Generic3x3ContainerScreen) {
         String title = handled.getTitle().getString().toLowerCase(Locale.ROOT);
         return title.contains("dropper") ? "dropper" : "dispenser";
      } else if (handled instanceof GenericContainerScreen containerScreen) {
         String title = handled.getTitle().getString().toLowerCase(Locale.ROOT);
         if (title.contains("barrel")) {
            return "barrel";
         } else if (title.contains("ender")) {
            return "ender_chest";
         } else {
            if (containerScreen.getScreenHandler() instanceof GenericContainerScreenHandler) {
               GenericContainerScreenHandler handler = (GenericContainerScreenHandler)containerScreen.getScreenHandler();
               if (handler.getRows() > 3) {
                  return "large_chest";
               }
            }

            return "chest";
         }
      } else if (handled instanceof FurnaceScreen) {
         return "furnace";
      } else if (handled instanceof BlastFurnaceScreen) {
         return "blast_furnace";
      } else if (handled instanceof SmokerScreen) {
         return "smoker";
      } else if (handled instanceof EnchantmentScreen) {
         return "enchanting_table";
      } else if (handled instanceof BrewingStandScreen) {
         return "brewing_stand";
      } else if (handled instanceof SmithingScreen) {
         return "smithing_table";
      } else if (handled instanceof GrindstoneScreen) {
         return "grindstone";
      } else if (handled instanceof StonecutterScreen) {
         return "stonecutter";
      } else if (handled instanceof CartographyTableScreen) {
         return "cartography_table";
      } else if (handled instanceof LoomScreen) {
         return "loom";
      } else if (handled instanceof HopperScreen) {
         return "hopper";
      } else if (handled instanceof MerchantScreen) {
         return "villager";
      } else if (handled instanceof HorseScreen) {
         return resolveMount(handled instanceof HorseScreenPovAccess horseScreen ? horseScreen.bbsPov$getEntity() : null);
      } else {
         return handled instanceof BeaconScreen ? "beacon" : null;
      }
   }

   public static String resolveMount(AbstractHorseEntity mount) {
      return !(mount instanceof DonkeyEntity) && !(mount instanceof MuleEntity) ? "horse" : "donkey";
   }
}
