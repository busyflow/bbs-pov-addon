package Glaxium.POV.actions.screeneffect.editor;

import Glaxium.POV.bodypart.PovBodyPartFolderSheet;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/** Collapsible folder header for a Screen Effect in the keyframe editor. */
public final class ScreenEffectFolderSheet extends PovBodyPartFolderSheet
{
    public ScreenEffectFolderSheet(String id, IKey title, int color)
    {
        super(id, title, new KeyframeChannel<>(id, KeyframeFactories.BOOLEAN));
        this.color = color;
    }

    public ScreenEffectFolderSheet(String id, IKey title, KeyframeChannel<?> channel)
    {
        super(id, title, channel != null ? channel : new KeyframeChannel<>(id, KeyframeFactories.BOOLEAN));
    }
}
