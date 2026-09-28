package Glaxium.POV.actions.gui.render;

import Glaxium.POV.actions.gui.render.common.GuiBackgroundRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import net.minecraft.item.ItemStack;

public interface GuiScreenChrome {
   GuiScreenChrome EMPTY = new GuiScreenChrome() {
   };

   default void drawEarlyChrome(GuiRenderContext ctx) {
   }

   default void drawBackground(GuiRenderContext ctx) {
      GuiBackgroundRenderer.drawDefault(ctx);
   }

   default ItemStack renderItems(GuiRenderContext ctx) {
      return GuiSlotRenderer.renderDefault(ctx);
   }

   default void drawLateItems(GuiRenderContext ctx) {
   }

   default void drawPreview(GuiRenderContext ctx) {
   }
}
