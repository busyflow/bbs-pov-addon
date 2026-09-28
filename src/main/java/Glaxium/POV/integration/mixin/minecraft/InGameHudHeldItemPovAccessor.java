package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.InGameHudHeldItemPovAccess;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({InGameHud.class})
public interface InGameHudHeldItemPovAccessor extends InGameHudHeldItemPovAccess {
   @Accessor("heldItemTooltipFade")
   @Override
   int bbsPov$getHeldItemTooltipFade();

   @Accessor("currentStack")
   @Override
   ItemStack bbsPov$getHeldItemTooltipStack();
}
