package Glaxium.POV.integration.access.minecraft;

import net.minecraft.item.ItemStack;

public interface LivingEntityPovAccess {
   ItemStack bbsPov$getActiveItemStack();

   void bbsPov$setActiveItemStack(ItemStack var1);

   int bbsPov$getItemUseTimeLeft();

   void bbsPov$setItemUseTimeLeft(int var1);

   void bbsPov$setLivingFlag(int var1, boolean var2);
}
