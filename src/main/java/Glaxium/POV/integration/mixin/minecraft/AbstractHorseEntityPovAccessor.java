package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.AbstractHorseEntityPovAccess;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.inventory.SimpleInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AbstractHorseEntity.class})
public interface AbstractHorseEntityPovAccessor extends AbstractHorseEntityPovAccess {
   @Accessor("items")
   @Override
   SimpleInventory bbsPov$getItems();

   @Invoker("updateSaddle")
   @Override
   void bbsPov$updateSaddle();

   @Invoker("setHorseFlag")
   @Override
   void bbsPov$setHorseFlag(int var1, boolean var2);
}
