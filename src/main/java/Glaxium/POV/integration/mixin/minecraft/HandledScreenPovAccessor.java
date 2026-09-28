package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HandledScreenPovAccess;
import java.util.Set;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({HandledScreen.class})
public interface HandledScreenPovAccessor extends HandledScreenPovAccess {
   @Accessor("cursorDragSlots")
   @Override
   Set<Slot> bbsPov$getCursorDragSlots();

   @Accessor("cursorDragging")
   @Override
   boolean bbsPov$isCursorDragging();

   @Accessor("draggedStackRemainder")
   @Override
   int bbsPov$getDraggedStackRemainder();

   @Accessor("heldButtonType")
   @Override
   int bbsPov$getHeldButtonType();
}
