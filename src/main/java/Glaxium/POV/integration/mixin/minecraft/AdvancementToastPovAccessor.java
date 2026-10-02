package Glaxium.POV.integration.mixin.minecraft;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.toast.AdvancementToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementToast.class)
public interface AdvancementToastPovAccessor
{
    @Accessor("advancement")
    AdvancementEntry bbsPov$getAdvancement();
}
