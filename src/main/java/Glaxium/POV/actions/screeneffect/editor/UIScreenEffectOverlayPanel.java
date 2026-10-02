package Glaxium.POV.actions.screeneffect.editor;

import Glaxium.POV.actions.screeneffect.ScreenEffectPresetEntry;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresets;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** Overlay panel for selecting a Screen Effect preset with live icons and labels. */
public class UIScreenEffectOverlayPanel extends UIOverlayPanel
{
    public static class UIScreenEffectList extends UIList<ScreenEffectPresetEntry>
    {
        public UIScreenEffectList(Consumer<List<ScreenEffectPresetEntry>> callback)
        {
            super(callback);
            this.scroll.scrollItemSize = 26;
        }

        @Override
        public void renderListElement(UIContext context, ScreenEffectPresetEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            RowStyle.row(context.batcher, x, y, this.area.w, h, this.rowColor(element), this.isHeader(element), hover, selected);

            this.renderElementPart(context, element, i, x, y, hover, selected);
        }

        @Override
        protected void renderElementPart(UIContext context, ScreenEffectPresetEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            DrawContext dc = context.batcher.getContext();
            if (dc != null)
            {
                ItemStack stack = element.createIconStack();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                RenderSystem.depthMask(true);
                dc.drawItem(stack, x + 4, y + 5);
                RenderSystem.disableDepthTest();
            }

            int textX = x + 24;
            context.batcher.text(element.name, textX, y + 3, 0xFFFFFFFF, false);
            context.batcher.text(element.description, textX, y + 13, 0xFFAAAAAA, false);
        }
    }

    public UISearchList<ScreenEffectPresetEntry> searchList;

    public UIScreenEffectOverlayPanel(IKey title, Consumer<ScreenEffectPresetEntry> callback)
    {
        super(title);

        UIScreenEffectList list = new UIScreenEffectList((selected) ->
        {
            if (callback != null && selected != null && !selected.isEmpty())
            {
                callback.accept(selected.get(0));
                this.close();
            }
        });
        list.add(ScreenEffectPresets.getAll());

        this.searchList = new UISearchList<>(list);
        this.searchList.relative(this.content).xy(6, 6).w(1F, -12).h(1F, -6);
        this.content.add(this.searchList);
    }
}
