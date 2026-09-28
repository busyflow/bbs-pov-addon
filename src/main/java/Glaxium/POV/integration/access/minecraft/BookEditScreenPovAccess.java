package Glaxium.POV.integration.access.minecraft;

import java.util.List;
import net.minecraft.client.util.SelectionManager;

public interface BookEditScreenPovAccess {
   List<String> bbsPov$getPages();

   int bbsPov$getCurrentPage();

   boolean bbsPov$isSigning();

   String bbsPov$getTitle();

   SelectionManager bbsPov$getPageSelection();

   SelectionManager bbsPov$getTitleSelection();
}
