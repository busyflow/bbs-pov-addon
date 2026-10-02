package Glaxium.POV.integration.access.minecraft;

import net.minecraft.util.Hand;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface ClientPlayerEntityPovAccess
{
    boolean bbsPov$isUsingItem();
    void bbsPov$setUsingItem(boolean value);
    Hand bbsPov$getClientActiveHand();
    void bbsPov$setClientActiveHand(Hand hand);
}
