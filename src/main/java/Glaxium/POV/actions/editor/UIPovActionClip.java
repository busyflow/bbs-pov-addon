package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.PovActionClip;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;

/** Minimal BBS-style inspector shared by every POV Action clip. */
public class UIPovActionClip<T extends PovActionClip> extends UIClip<T>
{
    public UIPovActionClip(T clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();
    }

    /** Match BBS replay actions: keep the common title/enabled and
     * layer/tick/duration rows, but omit camera envelopes. */
    @Override
    protected void addEnvelopes()
    {}

    @Override
    public void fillData()
    {
        super.fillData();
    }
}
