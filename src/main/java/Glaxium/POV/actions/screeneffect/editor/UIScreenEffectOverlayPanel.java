package Glaxium.POV.actions.screeneffect.editor;

import Glaxium.POV.actions.screeneffect.ScreenEffectPresetEntry;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresets;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.function.Consumer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public class UIScreenEffectOverlayPanel extends UIOverlayPanel {
   public UISearchList<ScreenEffectPresetEntry> searchList;

   public UIScreenEffectOverlayPanel(IKey title, Consumer<ScreenEffectPresetEntry> callback) {
      super(title);
      UIScreenEffectOverlayPanel.UIScreenEffectList list = new UIScreenEffectOverlayPanel.UIScreenEffectList(selected -> {
         if (callback != null && selected != null && !selected.isEmpty()) {
            callback.accept(selected.get(0));
            this.close();
         }
      });
      list.add(ScreenEffectPresets.getAll());
      this.searchList = new UISearchList(list);
      this.searchList.relative(this.content).xy(6, 6).w(1.0F, -12).h(1.0F, -6);
      this.content.add(this.searchList);
   }

   public static class UIScreenEffectList extends UIList<ScreenEffectPresetEntry> {
      public UIScreenEffectList(Consumer<List<ScreenEffectPresetEntry>> callback) {
         super(callback);
         this.scroll.scrollItemSize = 26;
      }

      public void renderListElement(UIContext context, ScreenEffectPresetEntry element, int i, int x, int y, boolean hover, boolean selected) {
         int h = this.scroll.scrollItemSize;
         RowStyle.row(context.batcher, x, y, this.area.w, h, this.rowColor(element), this.isHeader(element), hover, selected);
         this.renderElementPart(context, element, i, x, y, hover, selected);
      }

      protected void renderElementPart(UIContext context, ScreenEffectPresetEntry element, int i, int x, int y, boolean hover, boolean selected) {
         DrawContext dc = context.batcher.getContext();
         if (dc != null) {
            ItemStack stack = element.createIconStack();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
            dc.drawItem(stack, x + 4, y + 5);
            RenderSystem.disableDepthTest();
         }

         int textX = x + 24;
         context.batcher.text(element.name, (float)textX, (float)(y + 3), -1, false);
         context.batcher.text(element.description, (float)textX, (float)(y + 13), -5592406, false);
      }
   }
}
