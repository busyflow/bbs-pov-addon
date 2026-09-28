package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

public interface InGameHudVignettePovAccess {
   void bbsPov$renderVignetteOverlay(DrawContext var1, Entity var2);

   float bbsPov$getVignetteDarkness();

   float bbsPov$getSpyglassScale();
}
