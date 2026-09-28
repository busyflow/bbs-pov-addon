package Glaxium.POV.integration.access.minecraft;

import net.minecraft.util.Hand;

public interface ClientPlayerEntityPovAccess {
   boolean bbsPov$isUsingItem();

   void bbsPov$setUsingItem(boolean var1);

   Hand bbsPov$getClientActiveHand();

   void bbsPov$setClientActiveHand(Hand var1);
}
