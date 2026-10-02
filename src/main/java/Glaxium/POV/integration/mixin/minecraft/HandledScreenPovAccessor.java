package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.HandledScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(HandledScreen.class)
public interface HandledScreenPovAccessor extends HandledScreenPovAccess
{
    @Accessor("cursorDragSlots")
    Set<Slot> bbsPov$getCursorDragSlots();

    @Accessor("cursorDragging")
    boolean bbsPov$isCursorDragging();

    @Accessor("draggedStackRemainder")
    int bbsPov$getDraggedStackRemainder();

    @Accessor("heldButtonType")
    int bbsPov$getHeldButtonType();
}
