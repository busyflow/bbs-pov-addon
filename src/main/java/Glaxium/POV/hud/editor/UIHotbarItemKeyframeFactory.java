package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import net.minecraft.item.ItemStack;

/**
 * Replaces vanilla's own item-keyframe editor widget ({@code UIItemStackKeyframeFactory}) with
 * one that also has a count field. Vanilla's own {@code UIItemStack} widget (reused here for the
 * item-type picker -- drag-and-drop, right-click paste/reset, hotbar quick-pick, all untouched)
 * has no way to set the stack count at all, which is a real gap in BBS's own keyframe editing,
 * not something specific to this addon -- so this fixes it generally rather than working around
 * it just for Hotbar clips.
 *
 * <p>Registered via {@code UIKeyframeFactory.register(KeyframeFactories.ITEM_STACK, ...)} in
 * PovAddon#onInitializeClient, replacing vanilla's registration for that same factory type --
 * so this applies to every ItemStack keyframe in BBS's Film editor (Anchor's held-item keyframes,
 * etc.), not just POV tracks. That's a deliberate, disclosed choice: the count field is a
 * strict addition (nothing existing is removed or changed), so there's no reason to scope it down
 * to just one clip type even if that's what prompted building it.
 */
public class UIHotbarItemKeyframeFactory extends UIKeyframeFactory<ItemStack>
{
    private final UIItemStack itemPicker;
    private final UITrackpad count;
    private ItemStack current;

    public UIHotbarItemKeyframeFactory(Keyframe<ItemStack> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        ItemStack initial = keyframe.getValue();
        this.current = initial == null ? ItemStack.EMPTY : initial.copy();

        this.itemPicker = new UIItemStack(this::onItemPicked);
        this.itemPicker.setStack(this.current);

        this.count = new UITrackpad(this::onCountChanged);
        this.count.limit(1, 64, true);
        this.count.setValue(Math.max(1, this.current.getCount()));

        mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet sheet = null;
        if (editor != null && editor.getGraph() != null)
        {
            sheet = editor.getGraph().getSheet(keyframe);
        }
        if (sheet == null && editor != null && editor.getDopeSheet() != null)
        {
            sheet = editor.getDopeSheet().getSheet(keyframe);
        }
        String id = sheet != null && sheet.id != null ? sheet.id : (keyframe.getParent() != null ? keyframe.getParent().getId() : "");
        boolean isBlockChannel = "suffocation_block".equals(id) || (id != null && id.endsWith("_block"));

        this.scroll.add((IUIElement) this.itemPicker);
        if (!isBlockChannel)
        {
            this.scroll.add((IUIElement) this.count.marginTop(4));
        }
    }

    private void onItemPicked(ItemStack picked)
    {
        this.current = picked == null || picked.isEmpty() ? ItemStack.EMPTY : picked.copy();

        mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet sheet = null;
        if (this.editor != null && this.editor.getGraph() != null)
        {
            sheet = this.editor.getGraph().getSheet(this.keyframe);
        }
        if (sheet == null && this.editor != null && this.editor.getDopeSheet() != null)
        {
            sheet = this.editor.getDopeSheet().getSheet(this.keyframe);
        }
        String id = sheet != null && sheet.id != null ? sheet.id : (this.keyframe.getParent() != null ? this.keyframe.getParent().getId() : "");
        boolean isBlockChannel = "suffocation_block".equals(id) || (id != null && id.endsWith("_block"));

        if (!this.current.isEmpty() && !isBlockChannel)
        {
            this.current.setCount(clampCount(this.count.getValue()));
        }

        this.itemPicker.setStack(this.current);
        this.setValue(this.current);
    }

    private void onCountChanged(double value)
    {
        if (this.current.isEmpty())
        {
            return;
        }

        this.current.setCount(clampCount(value));
        this.itemPicker.setStack(this.current);
        this.setValue(this.current);
    }

    private static int clampCount(double value)
    {
        return Math.max(1, Math.min(64, (int) value));
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}
