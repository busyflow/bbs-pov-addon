package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Replaces vanilla's own Integer-keyframe editor widget ({@code UIIntegerKeyframeFactory}). Its
 * value field is a plain {@code UITrackpad} that's never told to be an integer field -- compare
 * to, say, {@code UIItemStack}'s count field elsewhere in this addon, which explicitly calls
 * {@code .limit(1, 64, true)} (the trailing {@code true} is exactly this "integer" flag). Without
 * it, {@code UITrackpad} freely drags and accepts typed text as a raw double: dragging the value
 * or typing e.g. "15.7" leaves the field showing "15.7" and keeps showing it after clicking away,
 * even though the same field is only ever bound to Integer-typed keyframe channels (health, hunger,
 * armor, air, etc. -- see {@code RecordedHudData}'s channel declarations). Nothing about that display
 * ever gets floored back to a whole number on its own.
 *
 * <p>This is exactly the bug reported: baking/recording produces clean integers because that path
 * writes {@code IntType} values directly (see {@code IntegerKeyframeFactory#fromData}/{@code
 * toData}), never touching this widget at all -- but manually dragging a keyframe's value in the
 * Film editor, or typing a number into this field, goes through this widget, which never rounds.
 *
 * <p>Registered via {@code UIKeyframeFactory.register(KeyframeFactories.INTEGER, ...)} in
 * PovAddon#onInitializeClient, replacing vanilla's registration for that same factory type --
 * same extension point already used for {@link UIHotbarItemKeyframeFactory}. That means this
 * applies to every Integer keyframe in BBS's Film editor, not just POV tracks -- a deliberate,
 * disclosed choice for the same reason as that class: this is a strict bugfix (round to the
 * nearest whole number, nothing removed or changed otherwise), so there's no reason to scope it
 * down to just this addon's own channels even though they're what prompted finding it.
 */
public class UIHotbarIntegerKeyframeFactory extends UIKeyframeFactory<Integer>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    public UIHotbarIntegerKeyframeFactory(Keyframe<Integer> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.value.integer();
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);

        this.scroll.add(this.value, this.handles.createColumn());
    }

    @Override
    public void update()
    {
        super.update();

        this.value.setValue(this.keyframe.getValue());
        this.handles.update();
    }
}
