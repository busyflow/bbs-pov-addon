package Glaxium.POV.actions.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiSlotSchema {
   private static final Map<String, GuiSlotSchema> SCHEMAS = new LinkedHashMap<>();
   public final String guiId;
   public final List<GuiSlotSchema.Slot> slots;
   public final boolean groupedSlots;
   public final int groupColumns;
   public final boolean playerInventory;

   private GuiSlotSchema(String guiId, List<GuiSlotSchema.Slot> slots, boolean groupedSlots, int groupColumns, boolean playerInventory) {
      this.guiId = guiId;
      this.slots = Collections.unmodifiableList(slots);
      this.groupedSlots = groupedSlots;
      this.groupColumns = groupColumns;
      this.playerInventory = playerInventory;
   }

   public static GuiSlotSchema get(String guiId) {
      return SCHEMAS.getOrDefault(guiId, SCHEMAS.get("inventory"));
   }

   public static List<GuiSlotSchema> getAll() {
      return List.copyOf(SCHEMAS.values());
   }

   public boolean isCraftingSlot(GuiSlotSchema.Slot slot) {
      return ("inventory".equals(this.guiId) || "crafting_table".equals(this.guiId)) && ("craft_result".equals(slot.id()) || slot.id().startsWith("craft_"));
   }

   public List<GuiSlotSchema.Slot> getCraftingSlots() {
      List<GuiSlotSchema.Slot> result = new ArrayList<>();

      for (GuiSlotSchema.Slot slot : this.slots) {
         if (this.isCraftingSlot(slot)) {
            result.add(slot);
         }
      }

      return result;
   }

   public boolean hasCraftingSlots() {
      return !this.getCraftingSlots().isEmpty();
   }

   public boolean isChestSlot(GuiSlotSchema.Slot slot) {
      return slot != null && slot.id().startsWith("chest_");
   }

   public boolean isEquipmentSlot(GuiSlotSchema.Slot slot) {
      return slot != null && ("saddle".equals(slot.id()) || "armor".equals(slot.id()));
   }

   public List<GuiSlotSchema.Slot> getGroupedSlots() {
      if (!this.groupedSlots) {
         return List.of();
      } else {
         List<GuiSlotSchema.Slot> grouped = new ArrayList<>();

         for (GuiSlotSchema.Slot slot : this.slots) {
            if (this.isChestSlot(slot) || slot.id().startsWith("container_")) {
               grouped.add(slot);
            }
         }

         return grouped.isEmpty() ? this.slots : grouped;
      }
   }

   public GuiSlotSchema.Slot slotByHandlerIndex(int handlerIndex) {
      for (GuiSlotSchema.Slot slot : this.slots) {
         if (slot.handlerIndex() == handlerIndex) {
            return slot;
         }
      }

      return null;
   }

   public static int playerInventoryStart(String guiId, int totalSlots) {
      return "inventory".equals(guiId) ? 9 : Math.max(0, totalSlots - 36);
   }

   private static void register(String guiId, boolean grouped, int columns, boolean playerInventory, GuiSlotSchema.Slot... slots) {
      SCHEMAS.put(guiId, new GuiSlotSchema(guiId, new ArrayList<>(List.of(slots)), grouped, columns, playerInventory));
   }

   private static GuiSlotSchema.Slot slot(String id, String label, int x, int y, int handlerIndex) {
      return new GuiSlotSchema.Slot(id, label, x, y, handlerIndex);
   }

   private static GuiSlotSchema.Slot[] grid(String prefix, String label, int count, int columns, int x, int y, int firstHandler) {
      GuiSlotSchema.Slot[] slots = new GuiSlotSchema.Slot[count];

      for (int i = 0; i < count; i++) {
         slots[i] = slot(prefix + i, label, x + i % columns * 18, y + i / columns * 18, firstHandler + i);
      }

      return slots;
   }

   private static GuiSlotSchema.Slot[] concat(GuiSlotSchema.Slot[] first, GuiSlotSchema.Slot[] second) {
      GuiSlotSchema.Slot[] result = new GuiSlotSchema.Slot[first.length + second.length];
      System.arraycopy(first, 0, result, 0, first.length);
      System.arraycopy(second, 0, result, first.length, second.length);
      return result;
   }

   static {
      register(
         "inventory",
         false,
         1,
         true,
         slot("craft_result", "Craft Result", 154, 28, 0),
         slot("craft_0", "Crafting", 98, 18, 1),
         slot("craft_1", "Crafting", 116, 18, 2),
         slot("craft_2", "Crafting", 98, 36, 3),
         slot("craft_3", "Crafting", 116, 36, 4),
         slot("armor_head", "Head Armor", 8, 8, 5),
         slot("armor_chest", "Chest Armor", 8, 26, 6),
         slot("armor_legs", "Leg Armor", 8, 44, 7),
         slot("armor_feet", "Feet Armor", 8, 62, 8),
         slot("offhand", "Offhand", 77, 62, 45)
      );
      register(
         "creative_inventory",
         false,
         1,
         true,
         slot("armor_head", "Head Armor", 54, 6, 5),
         slot("armor_chest", "Chest Armor", 54, 33, 6),
         slot("armor_legs", "Leg Armor", 108, 6, 7),
         slot("armor_feet", "Feet Armor", 108, 33, 8),
         slot("offhand", "Offhand", 35, 20, 45)
      );
      register(
         "crafting_table",
         false,
         1,
         true,
         concat(new GuiSlotSchema.Slot[]{slot("craft_result", "Craft Result", 124, 35, 0)}, grid("craft_", "Crafting", 9, 3, 30, 17, 1))
      );
      register("anvil", false, 1, true, slot("input_0", "Input", 27, 47, 0), slot("input_1", "Input", 76, 47, 1), slot("result", "Result", 134, 47, 2));
      register(
         "smithing_table",
         false,
         1,
         true,
         slot("template", "Template", 8, 48, 0),
         slot("base", "Base", 26, 48, 1),
         slot("addition", "Addition", 44, 48, 2),
         slot("result", "Result", 98, 48, 3)
      );
      register(
         "grindstone", false, 1, true, slot("top", "Top Input", 49, 19, 0), slot("bottom", "Bottom Input", 49, 40, 1), slot("result", "Result", 129, 34, 2)
      );
      register("stonecutter", false, 1, true, slot("input", "Input", 20, 33, 0), slot("result", "Result", 143, 33, 1));
      register(
         "cartography_table", false, 1, true, slot("map", "Map", 15, 15, 0), slot("addition", "Addition", 15, 52, 1), slot("result", "Result", 145, 39, 2)
      );
      register(
         "loom",
         false,
         1,
         true,
         slot("banner", "Banner", 13, 26, 0),
         slot("dye", "Dye", 33, 26, 1),
         slot("pattern", "Pattern", 23, 45, 2),
         slot("result", "Result", 143, 57, 3)
      );
      GuiSlotSchema.Slot[] furnace = new GuiSlotSchema.Slot[]{
         slot("input", "Input", 56, 17, 0), slot("fuel", "Fuel", 56, 53, 1), slot("result", "Result", 116, 35, 2)
      };
      register("furnace", false, 1, true, furnace);
      register("blast_furnace", false, 1, true, furnace);
      register("smoker", false, 1, true, furnace);
      register(
         "brewing_stand",
         false,
         1,
         true,
         slot("potion_0", "Potion", 56, 51, 0),
         slot("potion_1", "Potion", 79, 58, 1),
         slot("potion_2", "Potion", 102, 51, 2),
         slot("ingredient", "Ingredient", 79, 17, 3),
         slot("fuel", "Fuel", 17, 17, 4)
      );
      register("chest", true, 9, true, grid("container_", "Chest Slot", 27, 9, 8, 18, 0));
      register("barrel", true, 9, true, grid("container_", "Barrel Slot", 27, 9, 8, 18, 0));
      register("ender_chest", true, 9, true, grid("container_", "Ender Chest Slot", 27, 9, 8, 18, 0));
      register("shulker_box", true, 9, true, grid("container_", "Shulker Slot", 27, 9, 8, 18, 0));
      register("large_chest", true, 9, true, grid("container_", "Chest Slot", 54, 9, 8, 18, 0));
      register("hopper", true, 5, true, grid("container_", "Hopper Slot", 5, 5, 44, 20, 0));
      register("dispenser", true, 3, true, grid("container_", "Dispenser Slot", 9, 3, 62, 17, 0));
      register("dropper", true, 3, true, grid("container_", "Dropper Slot", 9, 3, 62, 17, 0));
      register("crafter", true, 3, true, grid("container_", "Crafter Slot", 9, 3, 26, 17, 0));
      register("enchanting_table", false, 1, true, slot("item", "Item", 15, 47, 0), slot("lapis", "Lapis", 35, 47, 1));
      register(
         "villager",
         false,
         1,
         true,
         slot("input_0", "Trade Input", 136, 37, 0),
         slot("input_1", "Trade Input", 162, 37, 1),
         slot("result", "Trade Result", 220, 37, 2)
      );
      register("horse", false, 1, true, slot("saddle", "Saddle", 8, 18, 0), slot("armor", "Armor", 8, 36, 1));
      register(
         "donkey", true, 5, true, concat(new GuiSlotSchema.Slot[]{slot("saddle", "Saddle", 8, 18, 0)}, grid("chest_", "Donkey Storage", 15, 5, 80, 18, 2))
      );
      register("beacon", false, 1, true, slot("payment", "Payment", 136, 110, 0));
      register("book", false, 1, false);
      register("gamemode_switcher", false, 1, false);
   }

   public static record Slot(String id, String label, int x, int y, int handlerIndex) {
   }
}
