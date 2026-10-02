package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.LivingEntityPovAccess;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityPovAccessor extends LivingEntityPovAccess
{
    @Accessor("activeItemStack") ItemStack bbsPov$getActiveItemStack();
    @Accessor("activeItemStack") void bbsPov$setActiveItemStack(ItemStack stack);
    @Accessor("itemUseTimeLeft") int bbsPov$getItemUseTimeLeft();
    @Accessor("itemUseTimeLeft") void bbsPov$setItemUseTimeLeft(int ticks);
    @Invoker("setLivingFlag") void bbsPov$setLivingFlag(int mask, boolean value);
}
