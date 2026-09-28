package Glaxium.POV.integration.access.minecraft;

import java.util.Set;
import net.minecraft.screen.slot.Slot;

public interface HandledScreenPovAccess {
   Set<Slot> bbsPov$getCursorDragSlots();

   boolean bbsPov$isCursorDragging();

   int bbsPov$getDraggedStackRemainder();

   int bbsPov$getHeldButtonType();
}
