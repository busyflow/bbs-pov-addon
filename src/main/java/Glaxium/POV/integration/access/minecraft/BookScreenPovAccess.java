package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.gui.screen.ingame.BookScreen;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface BookScreenPovAccess
{
    BookScreen.Contents bbsPov$getContents();
    int bbsPov$getPageIndex();
}
