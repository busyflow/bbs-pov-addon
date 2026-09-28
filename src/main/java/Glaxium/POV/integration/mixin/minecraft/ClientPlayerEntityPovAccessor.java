package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ClientPlayerEntityPovAccess;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientPlayerEntity.class})
public interface ClientPlayerEntityPovAccessor extends ClientPlayerEntityPovAccess {
   @Accessor("usingItem")
   @Override
   boolean bbsPov$isUsingItem();

   @Accessor("usingItem")
   @Override
   void bbsPov$setUsingItem(boolean var1);

   @Accessor("activeHand")
   @Override
   Hand bbsPov$getClientActiveHand();

   @Accessor("activeHand")
   @Override
   void bbsPov$setClientActiveHand(Hand var1);
}
