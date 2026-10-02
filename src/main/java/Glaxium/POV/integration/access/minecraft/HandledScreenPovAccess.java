package Glaxium.POV.integration.access.minecraft;

import net.minecraft.screen.slot.Slot;

import java.util.Set;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface HandledScreenPovAccess
{
    Set<Slot> bbsPov$getCursorDragSlots();
    boolean bbsPov$isCursorDragging();
    int bbsPov$getDraggedStackRemainder();
    int bbsPov$getHeldButtonType();
}
