package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIIntegerKeyframeFactory;

public class UIHotbarIntegerKeyframeFactory extends UIIntegerKeyframeFactory
{
    public UIHotbarIntegerKeyframeFactory(UITrackValue<Integer> track, UIKeyframes editor)
    {
        super(track, editor);
    }
}
