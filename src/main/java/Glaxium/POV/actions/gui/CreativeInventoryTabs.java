package Glaxium.POV.actions.gui;

import java.util.List;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;

public final class CreativeInventoryTabs {
   private CreativeInventoryTabs() {
   }

   public static List<ItemGroup> groups() {
      return ItemGroups.getGroupsToDisplay().stream().filter(group -> !group.getIcon().isOf(Items.COMMAND_BLOCK)).toList();
   }

   public static int page(ItemGroup group, int currentPage) {
      if (FabricCreativeGuiComponents.COMMON_GROUPS.contains(group)) {
         return currentPage;
      } else {
         return group instanceof FabricItemGroup fabricGroup ? fabricGroup.getPage() : 0;
      }
   }

   public static boolean visibleOnPage(ItemGroup group, int page) {
      return FabricCreativeGuiComponents.COMMON_GROUPS.contains(group) || page(group, page) == page;
   }

   public static int maxPage() {
      int max = 0;
      int nonCommon = 0;

      for (ItemGroup group : groups()) {
         if (!FabricCreativeGuiComponents.COMMON_GROUPS.contains(group)) {
            max = Math.max(max, page(group, 0));
         }
      }

      for (ItemGroup groupx : ItemGroups.getGroupsToDisplay()) {
         if (!FabricCreativeGuiComponents.COMMON_GROUPS.contains(groupx)) {
            nonCommon++;
         }
      }

      int countedPages = Math.max(0, (nonCommon + 9) / 10 - 1);
      return Math.max(max, countedPages);
   }
}
