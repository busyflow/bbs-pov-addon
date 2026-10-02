package Glaxium.POV.bodypart;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

/** A timeline folder row. It only owns expand/collapse state; it is never an
 * animatable property and therefore must not draw or accept keyframes. */
public class PovBodyPartFolderSheet extends UIKeyframeSheet
{
    public PovBodyPartFolderSheet(String id, IKey title, KeyframeChannel<?> channel)
    {
        super(id, title, 0x00000000, channel, null);
        this.section = new UIKeyframeSheet.Section(id, title, null, 0);
    }
}
