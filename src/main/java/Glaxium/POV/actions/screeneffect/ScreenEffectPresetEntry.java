package Glaxium.POV.actions.screeneffect;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ScreenEffectPresetEntry {
   public final String id;
   public final String name;
   public final String description;
   public final String iconItemId;
   public final float defaultIntensity;
   public final int vanillaRenderOrder;

   public ScreenEffectPresetEntry(String id, String name, String description, String iconItemId, float defaultIntensity, int vanillaRenderOrder) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.iconItemId = iconItemId;
      this.defaultIntensity = defaultIntensity;
      this.vanillaRenderOrder = vanillaRenderOrder;
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
}
