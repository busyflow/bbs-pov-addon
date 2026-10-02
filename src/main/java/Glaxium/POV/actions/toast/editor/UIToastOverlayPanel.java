package Glaxium.POV.actions.toast.editor;

import Glaxium.POV.actions.toast.ToastPresets;
import Glaxium.POV.actions.toast.ToastTypeEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Overlay panel for selecting a Toast preset with live icons and labels. */
public class UIToastOverlayPanel extends UIOverlayPanel
{
    public static class UIToastList extends UIList<ToastTypeEntry>
    {
        public UIToastList(Consumer<List<ToastTypeEntry>> callback)
        {
            super(callback);
            this.scroll.scrollItemSize = 26;
        }

        @Override
        public void renderListElement(UIContext context, ToastTypeEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            RowStyle.row(context.batcher, x, y, this.area.w, h, this.rowColor(element), this.isHeader(element), hover, selected);

            this.renderElementPart(context, element, i, x, y, hover, selected);
        }

        @Override
        protected void renderElementPart(UIContext context, ToastTypeEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            int iconX = x + 4;
            int iconY = y + (h - 16) / 2;

            // Black square behind item icon
            context.batcher.box(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xCC000000);

            DrawContext dc = context.batcher.getContext();
            if (dc != null && element != null)
            {
                ItemStack stack = element.createIconStack();
                RenderSystem.enableBlend();
                dc.drawItem(stack, iconX, iconY);
            }

            int textX = x + 26;
            if (element != null)
            {
                String title = element.title;
                String desc = element.description + " (" + element.category + ")";

                context.batcher.text(title, textX, y + 3, element.getTitleColor(), false);
                context.batcher.text(desc, textX, y + 13, 0xFFAAAAAA, false);
            }
        }

        @Override
        protected String elementToString(UIContext context, int i, ToastTypeEntry element)
        {
            if (element == null) return "";
            return element.title + " " + element.description + " " + element.category;
        }
    }

    public UISearchList<ToastTypeEntry> list;
    public Consumer<ToastTypeEntry> callback;

    public UIToastOverlayPanel(IKey title, Consumer<ToastTypeEntry> callback)
    {
        super(title);
        this.callback = callback;

        UIToastList toastList = new UIToastList((selected) ->
        {
            if (this.callback != null && selected != null && !selected.isEmpty())
            {
                this.callback.accept(selected.get(0));
                this.close();
            }
        });
        toastList.add(ToastPresets.getAll());

        this.list = new UISearchList<>(toastList);
        this.list.relative(this.content).xy(6, 6).w(1F, -12).h(1F, -6);
        this.content.add(this.list);
    }
}
