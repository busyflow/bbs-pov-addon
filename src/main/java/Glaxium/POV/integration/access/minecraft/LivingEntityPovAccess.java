package Glaxium.POV.integration.access.minecraft;

import net.minecraft.item.ItemStack;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface LivingEntityPovAccess
{
    ItemStack bbsPov$getActiveItemStack();
    void bbsPov$setActiveItemStack(ItemStack stack);
    int bbsPov$getItemUseTimeLeft();
    void bbsPov$setItemUseTimeLeft(int ticks);
    void bbsPov$setLivingFlag(int mask, boolean value);
}
