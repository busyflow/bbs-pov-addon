package Glaxium.POV.actions.gui.clip;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.item.ItemStack;

public final class GuiSlotChannels {
   private GuiSlotChannels() {
   }

   public static List<KeyframeChannel<ItemStack>> containerSlots(GuiPovActionClip clip, String guiTypeId) {
      List<KeyframeChannel<ItemStack>> list = new ArrayList<>();
      if ("crafting_table".equals(guiTypeId)) {
         for (int i = 0; i < 9; i++) {
            list.add(clip.getNamedSlot("crafting_slot_" + i));
         }

         list.add(clip.getNamedSlot("craft_result_slot"));
      } else if ("inventory".equals(guiTypeId)) {
         for (int i = 0; i < 4; i++) {
            list.add(clip.getNamedSlot("crafting_slot_" + i));
         }

         list.add(clip.getNamedSlot("craft_result_slot"));
      } else if ("anvil".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("anvil_input_0"));
         list.add(clip.getNamedSlot("anvil_input_1"));
         list.add(clip.getNamedSlot("anvil_result"));
      } else if ("furnace".equals(guiTypeId) || "blast_furnace".equals(guiTypeId) || "smoker".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("furnace_input"));
         list.add(clip.getNamedSlot("furnace_fuel"));
         list.add(clip.getNamedSlot("furnace_result"));
      } else if ("enchanting_table".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("enchant_item"));
         list.add(clip.getNamedSlot("enchant_lapis"));
      } else if ("brewing_stand".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("brewing_potion_0"));
         list.add(clip.getNamedSlot("brewing_potion_1"));
         list.add(clip.getNamedSlot("brewing_potion_2"));
         list.add(clip.getNamedSlot("brewing_ingredient"));
         list.add(clip.getNamedSlot("brewing_fuel"));
      } else if ("smithing_table".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("smithing_template"));
         list.add(clip.getNamedSlot("smithing_base"));
         list.add(clip.getNamedSlot("smithing_addition"));
         list.add(clip.getNamedSlot("smithing_result"));
      } else if ("grindstone".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("grindstone_top"));
         list.add(clip.getNamedSlot("grindstone_bottom"));
         list.add(clip.getNamedSlot("grindstone_result"));
      } else if ("stonecutter".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("stonecutter_input"));
         list.add(clip.getNamedSlot("stonecutter_result"));
      } else if ("cartography_table".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("cartography_map"));
         list.add(clip.getNamedSlot("cartography_addition"));
         list.add(clip.getNamedSlot("cartography_result"));
      } else if ("loom".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("loom_banner"));
         list.add(clip.getNamedSlot("loom_dye"));
         list.add(clip.getNamedSlot("loom_pattern"));
         list.add(clip.getNamedSlot("loom_result"));
      } else if ("large_chest".equals(guiTypeId)) {
         for (int i = 0; i < 54; i++) {
            list.add(clip.getNamedSlot("container_slot_" + i));
         }
      } else if ("chest".equals(guiTypeId) || "barrel".equals(guiTypeId) || "ender_chest".equals(guiTypeId) || "shulker_box".equals(guiTypeId)) {
         for (int i = 0; i < 27; i++) {
            list.add(clip.getNamedSlot("container_slot_" + i));
         }
      } else if ("hopper".equals(guiTypeId)) {
         for (int i = 0; i < 5; i++) {
            list.add(clip.getNamedSlot("container_slot_" + i));
         }
      } else if ("dispenser".equals(guiTypeId) || "dropper".equals(guiTypeId) || "crafter".equals(guiTypeId)) {
         for (int i = 0; i < 9; i++) {
            list.add(clip.getNamedSlot("container_slot_" + i));
         }
      } else if ("villager".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("villager_input_1"));
         list.add(clip.getNamedSlot("villager_input_2"));
         list.add(clip.getNamedSlot("villager_result"));
      } else if ("horse".equals(guiTypeId)) {
         list.add(clip.getNamedSlot("horse_saddle"));
         list.add(clip.getNamedSlot("horse_armor"));
      }

      return list;
   }

   public static KeyframeChannel<ItemStack> legacySlot(GuiPovActionClip clip, String guiId, String slotId) {
      if (slotId.startsWith("container_")) {
         return clip.namedSlots.get("container_slot_" + slotId.substring(10));
      } else if (slotId.startsWith("craft_")) {
         return clip.namedSlots.get("crafting_slot_" + slotId.substring(6));
      } else if ("craft_result".equals(slotId)) {
         return clip.namedSlots.get("craft_result_slot");
      } else {
         String prefix = switch (guiId) {
            case "anvil" -> "anvil_";
            case "furnace", "blast_furnace", "smoker" -> "furnace_";
            case "enchanting_table" -> "enchant_";
            case "brewing_stand" -> "brewing_";
            case "smithing_table" -> "smithing_";
            case "grindstone" -> "grindstone_";
            case "stonecutter" -> "stonecutter_";
            case "cartography_table" -> "cartography_";
            case "loom" -> "loom_";
            case "villager" -> "villager_";
            case "horse" -> "horse_";
            default -> "";
         };
         String legacyId = prefix + slotId;
         if ("inventory".equals(guiId) && slotId.startsWith("craft_")) {
            legacyId = "crafting_slot_" + slotId.substring(6);
         }

         if ("inventory".equals(guiId) && "craft_result".equals(slotId)) {
            legacyId = "craft_result_slot";
         }

         if ("enchanting_table".equals(guiId) && "item".equals(slotId)) {
            legacyId = "enchant_item";
         }

         if ("smithing_table".equals(guiId) && "input_0".equals(slotId)) {
            legacyId = "smithing_template";
         }

         return clip.namedSlots.get(legacyId);
      }
   }
}
