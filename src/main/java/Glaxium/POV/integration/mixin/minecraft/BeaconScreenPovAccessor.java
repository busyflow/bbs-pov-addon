package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.BeaconScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.BeaconScreen;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeaconScreen.class)
public interface BeaconScreenPovAccessor extends BeaconScreenPovAccess
{
    @Accessor("primaryEffect")
    StatusEffect bbsPov$getPrimaryEffect();

    @Accessor("secondaryEffect")
    StatusEffect bbsPov$getSecondaryEffect();
}
