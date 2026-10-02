package Glaxium.POV.integration.access.minecraft;

import net.minecraft.entity.effect.StatusEffect;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface BeaconScreenPovAccess
{
    StatusEffect bbsPov$getPrimaryEffect();
    StatusEffect bbsPov$getSecondaryEffect();
}
