package Glaxium.POV.actions.toast;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ToastTypeEntry {
   public final String id;
   public final String category;
   public final String title;
   public final String description;
   public final String iconItemId;
   public final String frameType;
   public final String textureId;

   public ToastTypeEntry(String id, String category, String title, String description, String iconItemId, String frameType, String textureId) {
      this.id = id;
      this.category = category;
      this.title = title;
      this.description = description;
      this.iconItemId = iconItemId;
      this.frameType = frameType;
      this.textureId = textureId;
   }

   public ItemStack createIconStack() {
      if (this.iconItemId != null && !this.iconItemId.isEmpty()) {
         try {
            Item item = (Item)Registries.ITEM.get(new Identifier(this.iconItemId));
            if (item != null && item != Items.AIR) {
               return new ItemStack(item);
            }
         } catch (Exception var2) {
         }
      }

      return new ItemStack(Items.DIAMOND);
   }

   public int getTitleColor() {
      if ("challenge".equalsIgnoreCase(this.frameType)) {
         return -43521;
      } else if ("goal".equalsIgnoreCase(this.frameType)) {
         return -11141121;
      } else if ("recipe".equalsIgnoreCase(this.frameType)) {
         return -11534256;
      } else {
         return "system".equalsIgnoreCase(this.frameType) ? -1 : -171;
      }
   }
}
