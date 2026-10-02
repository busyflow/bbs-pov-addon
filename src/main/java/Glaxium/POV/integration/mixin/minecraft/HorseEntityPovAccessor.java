package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HorseEntityPovAccess;

import net.minecraft.entity.passive.HorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HorseEntity.class)
public interface HorseEntityPovAccessor extends HorseEntityPovAccess
{
    @Invoker("setHorseVariant")
    void bbsPov$setHorseVariant(int variant);
}
