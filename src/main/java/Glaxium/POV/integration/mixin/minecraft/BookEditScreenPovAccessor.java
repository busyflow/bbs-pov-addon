package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.BookEditScreenPovAccess;

import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BookEditScreen.class)
public interface BookEditScreenPovAccessor extends BookEditScreenPovAccess
{
    @Accessor("pages")
    List<String> bbsPov$getPages();

    @Accessor("currentPage")
    int bbsPov$getCurrentPage();

    @Accessor("signing")
    boolean bbsPov$isSigning();

    @Accessor("title")
    String bbsPov$getTitle();

    @Accessor("currentPageSelectionManager")
    SelectionManager bbsPov$getPageSelection();

    @Accessor("bookTitleSelectionManager")
    SelectionManager bbsPov$getTitleSelection();
}
