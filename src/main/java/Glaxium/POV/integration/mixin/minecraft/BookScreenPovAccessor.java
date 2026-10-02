package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.BookScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.BookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BookScreen.class)
public interface BookScreenPovAccessor extends BookScreenPovAccess
{
    @Accessor("contents")
    BookScreen.Contents bbsPov$getContents();

    @Accessor("pageIndex")
    int bbsPov$getPageIndex();
}
