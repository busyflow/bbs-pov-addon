package Glaxium.POV.integration.access.minecraft;

import net.minecraft.inventory.SimpleInventory;

public interface AbstractHorseEntityPovAccess {
   SimpleInventory bbsPov$getItems();

   void bbsPov$updateSaddle();

   void bbsPov$setHorseFlag(int var1, boolean var2);
}
