package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.util.SelectionManager;

import java.util.List;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface BookEditScreenPovAccess
{
    List<String> bbsPov$getPages();
    int bbsPov$getCurrentPage();
    boolean bbsPov$isSigning();
    String bbsPov$getTitle();
    SelectionManager bbsPov$getPageSelection();
    SelectionManager bbsPov$getTitleSelection();
}
