package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.MerchantScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface MerchantScreenPovAccessor extends MerchantScreenPovAccess
{
    @Accessor("selectedIndex")
    int bbsPov$getSelectedIndex();

    @Accessor("indexStartOffset")
    int bbsPov$getIndexStartOffset();
}
