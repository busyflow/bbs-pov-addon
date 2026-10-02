package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HorseScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.passive.AbstractHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseScreen.class)
public interface HorseScreenPovAccessor extends HorseScreenPovAccess
{
    @Accessor("entity")
    AbstractHorseEntity bbsPov$getEntity();
}
