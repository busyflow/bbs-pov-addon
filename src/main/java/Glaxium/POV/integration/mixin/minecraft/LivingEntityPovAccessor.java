package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.LivingEntityPovAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LivingEntity.class})
public interface LivingEntityPovAccessor extends LivingEntityPovAccess {
   @Accessor("activeItemStack")
   @Override
   ItemStack bbsPov$getActiveItemStack();

   @Accessor("activeItemStack")
   @Override
   void bbsPov$setActiveItemStack(ItemStack var1);

   @Accessor("itemUseTimeLeft")
   @Override
   int bbsPov$getItemUseTimeLeft();

   @Accessor("itemUseTimeLeft")
   @Override
   void bbsPov$setItemUseTimeLeft(int var1);

   @Invoker("setLivingFlag")
   @Override
   void bbsPov$setLivingFlag(int var1, boolean var2);
}
