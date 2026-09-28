package Glaxium.POV.actions.gui.editor;

import Glaxium.POV.actions.gui.GuiTypeEntry;
import java.util.function.Consumer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;

public class UIGuiTypeOverlayPanel extends UIOverlayPanel {
   public final UISearchList<GuiTypeEntry> list;
   private final Consumer<GuiTypeEntry> callback;

   public UIGuiTypeOverlayPanel(Consumer<GuiTypeEntry> callback, String currentId) {
      super(IKey.constant("GUI Type"));
      this.callback = callback;
      UIGuiTypeList typeList = new UIGuiTypeList(selected -> {
         if (selected != null && !selected.isEmpty() && this.callback != null) {
            this.callback.accept(selected.get(0));
            this.close();
         }
      });
      this.list = new UISearchList(typeList);
      this.list.label(IKey.constant("Search..."));
      this.list.list.background();
      this.list.list.add(GuiTypeEntry.getAll());
      GuiTypeEntry current = GuiTypeEntry.findById(currentId);
      if (current != null) {
         this.list.list.setCurrentScroll(current);
      }

      this.list.relative(this.content).xy(6, 6).w(1.0F, -12).h(1.0F, -12);
      this.content.add(this.list);
   }

   protected void onAdd(UIElement parent) {
      super.onAdd(parent);
      if (this.getContext() != null && this.list != null && this.list.search != null) {
         this.getContext().focus(this.list.search);
      }
   }
}
