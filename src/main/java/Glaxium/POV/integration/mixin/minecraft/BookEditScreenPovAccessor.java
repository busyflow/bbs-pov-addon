package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.BookEditScreenPovAccess;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.SelectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BookEditScreen.class})
public interface BookEditScreenPovAccessor extends BookEditScreenPovAccess {
   @Accessor("pages")
   @Override
   List<String> bbsPov$getPages();

   @Accessor("currentPage")
   @Override
   int bbsPov$getCurrentPage();

   @Accessor("signing")
   @Override
   boolean bbsPov$isSigning();

   @Accessor("title")
   @Override
   String bbsPov$getTitle();

   @Accessor("currentPageSelectionManager")
   @Override
   SelectionManager bbsPov$getPageSelection();

   @Accessor("bookTitleSelectionManager")
   @Override
   SelectionManager bbsPov$getTitleSelection();
}
