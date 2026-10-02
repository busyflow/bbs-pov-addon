package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;

import net.minecraft.item.ItemStack;

/**
 * Replaces vanilla's own item-keyframe editor widget ({@code UIItemStackKeyframeFactory}) with
 * one that also has a count field.
 */
public class UIHotbarItemKeyframeFactory extends UIKeyframeFactory<ItemStack>
{
    private final UIItemStack itemPicker;
    private final UITrackpad count;
    private ItemStack current;

    public UIHotbarItemKeyframeFactory(UITrackValue<ItemStack> track, UIKeyframes editor)
    {
        super(track, editor);

        ItemStack initial = track.getValue();
        this.current = initial == null ? ItemStack.EMPTY : initial.copy();

        this.itemPicker = new UIItemStack(this::onItemPicked);
        this.itemPicker.setStack(this.current);

        this.count = new UITrackpad(this::onCountChanged);
        this.count.limit(1, 64, true);
        this.count.setValue(Math.max(1, this.current.getCount()));

        UIKeyframeSheet sheet = track.sheet;
        String id = sheet != null && sheet.id != null ? sheet.id : "";
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

        UIKeyframeSheet sheet = this.track.sheet;
        String id = sheet != null && sheet.id != null ? sheet.id : "";
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
    public void update()
    {
        super.update();
        ItemStack val = this.getDisplayValue();
        this.current = val == null ? ItemStack.EMPTY : val.copy();
        this.itemPicker.setStack(this.current);
        this.count.setValue(Math.max(1, this.current.getCount()));
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff141414);
        super.render(context);
    }
}
