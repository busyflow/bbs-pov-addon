package Glaxium.POV.integration.access.minecraft;

import net.minecraft.entity.passive.AbstractHorseEntity;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface HorseScreenPovAccess
{
    AbstractHorseEntity bbsPov$getEntity();
}
