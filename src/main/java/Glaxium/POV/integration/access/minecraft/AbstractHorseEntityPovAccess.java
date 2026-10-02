package Glaxium.POV.integration.access.minecraft;

import net.minecraft.inventory.SimpleInventory;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface AbstractHorseEntityPovAccess
{
    SimpleInventory bbsPov$getItems();
    void bbsPov$updateSaddle();
    void bbsPov$setHorseFlag(int flag, boolean value);
}
