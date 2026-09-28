package Glaxium.POV.actions.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public final class GuiTypeEntry {
   private static final Map<String, GuiTypeEntry> REGISTRY = new LinkedHashMap<>();
   private static final List<GuiTypeEntry> ALL_ENTRIES = new ArrayList<>();
   public final String id;
   public final String name;
   public final String category;
   public final Identifier texture;
   public final int u;
   public final int v;
   public final int regionWidth;
   public final int regionHeight;
   public final int textureWidth;
   public final int textureHeight;
   public final String title;
   public final int titleX;
   public final int titleY;
   public final int inventoryTitleX;
   public final int inventoryTitleY;
   public static final GuiTypeEntry INVENTORY = register(
      "inventory", "Inventory", "Player", "textures/gui/container/inventory.png", 0, 0, 176, 166, 256, 256, "Crafting", 97, 6
   );
   public static final GuiTypeEntry CREATIVE = register(
      "creative_inventory", "Creative Inventory", "Player", "textures/gui/container/creative_inventory/tab_items.png", 0, 0, 195, 136, 256, 256, "", 0, 0
   );
   public static final GuiTypeEntry CRAFTING_TABLE = register(
      "crafting_table", "Crafting Table", "Crafting", "textures/gui/container/crafting_table.png", 0, 0, 176, 166, 256, 256, "Crafting", 29, 6
   );
   public static final GuiTypeEntry ANVIL = register(
      "anvil", "Anvil", "Crafting", "textures/gui/container/anvil.png", 0, 0, 176, 166, 256, 256, "Repair & Name", 60, 6
   );
   public static final GuiTypeEntry SMITHING_TABLE = register(
      "smithing_table", "Smithing Table", "Crafting", "textures/gui/container/smithing.png", 0, 0, 176, 166, 256, 256, "Upgrade Gear", 44, 15
   );
   public static final GuiTypeEntry GRINDSTONE = register(
      "grindstone", "Grindstone", "Crafting", "textures/gui/container/grindstone.png", 0, 0, 176, 166, 256, 256, "Repair & Disenchant", 8, 6
   );
   public static final GuiTypeEntry STONECUTTER = register(
      "stonecutter", "Stonecutter", "Crafting", "textures/gui/container/stonecutter.png", 0, 0, 176, 166, 256, 256, "Stonecutter", 8, 6
   );
   public static final GuiTypeEntry CARTOGRAPHY_TABLE = register(
      "cartography_table", "Cartography Table", "Crafting", "textures/gui/container/cartography_table.png", 0, 0, 176, 166, 256, 256, "Cartography Table", 8, 4
   );
   public static final GuiTypeEntry LOOM = register("loom", "Loom", "Crafting", "textures/gui/container/loom.png", 0, 0, 176, 166, 256, 256, "Loom", 8, 6);
   public static final GuiTypeEntry FURNACE = register(
      "furnace", "Furnace", "Smelting", "textures/gui/container/furnace.png", 0, 0, 176, 166, 256, 256, "Furnace", -1, 6
   );
   public static final GuiTypeEntry BLAST_FURNACE = register(
      "blast_furnace", "Blast Furnace", "Smelting", "textures/gui/container/blast_furnace.png", 0, 0, 176, 166, 256, 256, "Blast Furnace", -1, 6
   );
   public static final GuiTypeEntry SMOKER = register(
      "smoker", "Smoker", "Smelting", "textures/gui/container/smoker.png", 0, 0, 176, 166, 256, 256, "Smoker", -1, 6
   );
   public static final GuiTypeEntry BREWING_STAND = register(
      "brewing_stand", "Brewing Stand", "Brewing", "textures/gui/container/brewing_stand.png", 0, 0, 176, 166, 256, 256, "Brewing Stand", -1, 6
   );
   public static final GuiTypeEntry CHEST_SMALL = register(
      "chest", "Chest (Small 9x3)", "Containers", "textures/gui/container/generic_54.png", 0, 0, 176, 168, 256, 256, "Chest", 8, 6
   );
   public static final GuiTypeEntry CHEST_LARGE = register(
      "large_chest", "Chest (Large 9x6)", "Containers", "textures/gui/container/generic_54.png", 0, 0, 176, 222, 256, 256, "Large Chest", 8, 6
   );
   public static final GuiTypeEntry BARREL = register(
      "barrel", "Barrel", "Containers", "textures/gui/container/generic_54.png", 0, 0, 176, 168, 256, 256, "Barrel", 8, 6
   );
   public static final GuiTypeEntry ENDER_CHEST = register(
      "ender_chest", "Ender Chest", "Containers", "textures/gui/container/generic_54.png", 0, 0, 176, 168, 256, 256, "Ender Chest", 8, 6
   );
   public static final GuiTypeEntry SHULKER_BOX = register(
      "shulker_box", "Shulker Box", "Containers", "textures/gui/container/shulker_box.png", 0, 0, 176, 166, 256, 256, "Shulker Box", 8, 6
   );
   public static final GuiTypeEntry HOPPER = register(
      "hopper", "Hopper", "Containers", "textures/gui/container/hopper.png", 0, 0, 176, 133, 256, 256, "Item Hopper", 8, 6
   );
   public static final GuiTypeEntry DISPENSER = register(
      "dispenser", "Dispenser", "Redstone", "textures/gui/container/dispenser.png", 0, 0, 176, 166, 256, 256, "Dispenser", -1, 6
   );
   public static final GuiTypeEntry DROPPER = register(
      "dropper", "Dropper", "Redstone", "textures/gui/container/dispenser.png", 0, 0, 176, 166, 256, 256, "Dropper", -1, 6
   );
   public static final GuiTypeEntry ENCHANTING_TABLE = register(
      "enchanting_table", "Enchanting Table", "Enchanting", "textures/gui/container/enchanting_table.png", 0, 0, 176, 166, 256, 256, "Enchant", 8, 5
   );
   public static final GuiTypeEntry BEACON = register("beacon", "Beacon", "Special", "textures/gui/container/beacon.png", 0, 0, 230, 219, 256, 256, "", 0, 0);
   public static final GuiTypeEntry VILLAGER = register(
      "villager", "Villager Trading", "Trading", "textures/gui/container/villager.png", 0, 0, 276, 166, 512, 256, "Merchant", 136, 6
   );
   public static final GuiTypeEntry HORSE = register(
      "horse", "Horse Inventory", "Entities", "textures/gui/container/horse.png", 0, 0, 176, 166, 256, 256, "Horse", 8, 6
   );
   public static final GuiTypeEntry DONKEY = register(
      "donkey", "Donkey Inventory", "Entities", "textures/gui/container/horse.png", 0, 0, 176, 166, 256, 256, "Donkey", 8, 6
   );
   public static final int BOOK_SCREEN_Y = 2;
   public static final GuiTypeEntry BOOK = register("book", "Book", "Special", "textures/gui/book.png", 0, 0, 192, 220, 256, 256, "", 0, 0);
   public static final GuiTypeEntry GAMEMODE_SWITCHER = register(
      "gamemode_switcher", "Gamemode Switcher", "Special", "textures/gui/container/gamemode_switcher.png", 0, 0, 125, 75, 128, 128, "", 0, 0
   );

   public GuiTypeEntry(
      String id,
      String name,
      String category,
      Identifier texture,
      int u,
      int v,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      String title,
      int titleX,
      int titleY
   ) {
      this.id = id;
      this.name = name;
      this.category = category;
      this.texture = texture;
      this.u = u;
      this.v = v;
      this.regionWidth = regionWidth;
      this.regionHeight = regionHeight;
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
      this.title = title;
      this.titleX = titleX;
      this.titleY = titleY;
      this.inventoryTitleX = inventoryTitleX(id);
      this.inventoryTitleY = this.inventoryTitleX < 0 ? -1 : regionHeight - 94;
   }

   private static int inventoryTitleX(String id) {
      return switch (id) {
         case "inventory", "creative_inventory", "beacon", "book", "gamemode_switcher" -> -1;
         case "villager" -> 107;
         default -> 8;
      };
   }

   public ItemStack getIcon() {
      String var1 = this.id;

      return switch (var1) {
         case "inventory" -> new ItemStack(Items.CHEST);
         case "creative_inventory" -> new ItemStack(Items.COMPASS);
         case "crafting_table" -> new ItemStack(Items.CRAFTING_TABLE);
         case "anvil" -> new ItemStack(Items.ANVIL);
         case "smithing_table" -> new ItemStack(Items.SMITHING_TABLE);
         case "grindstone" -> new ItemStack(Items.GRINDSTONE);
         case "stonecutter" -> new ItemStack(Items.STONECUTTER);
         case "cartography_table" -> new ItemStack(Items.CARTOGRAPHY_TABLE);
         case "loom" -> new ItemStack(Items.LOOM);
         case "furnace" -> new ItemStack(Items.FURNACE);
         case "blast_furnace" -> new ItemStack(Items.BLAST_FURNACE);
         case "smoker" -> new ItemStack(Items.SMOKER);
         case "brewing_stand" -> new ItemStack(Items.BREWING_STAND);
         case "chest", "large_chest" -> new ItemStack(Items.CHEST);
         case "barrel" -> new ItemStack(Items.BARREL);
         case "ender_chest" -> new ItemStack(Items.ENDER_CHEST);
         case "shulker_box" -> new ItemStack(Items.SHULKER_BOX);
         case "hopper" -> new ItemStack(Items.HOPPER);
         case "dispenser" -> new ItemStack(Items.DISPENSER);
         case "dropper" -> new ItemStack(Items.DROPPER);
         case "enchanting_table" -> new ItemStack(Items.ENCHANTING_TABLE);
         case "beacon" -> new ItemStack(Items.BEACON);
         case "villager" -> new ItemStack(Items.EMERALD);
         case "horse" -> new ItemStack(Items.SADDLE);
         case "donkey" -> new ItemStack(Items.CHEST);
         case "book" -> new ItemStack(Items.WRITABLE_BOOK);
         case "gamemode_switcher" -> new ItemStack(Items.COMMAND_BLOCK);
         default -> new ItemStack(Items.CHEST);
      };
   }

   public static int getCategoryColor(String category) {
      String var1 = category == null ? "" : category;

      return switch (var1) {
         case "Player" -> -13928234;
         case "Crafting" -> -2525148;
         case "Smelting" -> -2078682;
         case "Brewing" -> -6735144;
         case "Containers" -> -7577031;
         case "Redstone" -> -2742232;
         case "Enchanting" -> -4708680;
         case "Special" -> -2843885;
         case "Trading" -> -14305204;
         case "Entities" -> -6269924;
         default -> -7829368;
      };
   }

   public static GuiTypeEntry register(
      String id,
      String name,
      String category,
      String texturePath,
      int u,
      int v,
      int regionWidth,
      int regionHeight,
      int textureWidth,
      int textureHeight,
      String title,
      int titleX,
      int titleY
   ) {
      Identifier tex = new Identifier("minecraft", texturePath);
      GuiTypeEntry entry = new GuiTypeEntry(id, name, category, tex, u, v, regionWidth, regionHeight, textureWidth, textureHeight, title, titleX, titleY);
      REGISTRY.put(id, entry);
      ALL_ENTRIES.add(entry);
      return entry;
   }

   public static List<GuiTypeEntry> getAll() {
      return Collections.unmodifiableList(ALL_ENTRIES);
   }

   public static GuiTypeEntry findById(String id) {
      if (id != null && !id.isBlank()) {
         GuiTypeEntry entry = REGISTRY.get(id);
         if (entry != null) {
            return entry;
         } else {
            for (GuiTypeEntry e : ALL_ENTRIES) {
               if (e.id.equalsIgnoreCase(id) || e.id.endsWith(":" + id) || id.endsWith(":" + e.id)) {
                  return e;
               }
            }

            return INVENTORY;
         }
      } else {
         return INVENTORY;
      }
   }

   @Override
   public String toString() {
      return this.name + " " + this.id + " " + this.category;
   }
}
