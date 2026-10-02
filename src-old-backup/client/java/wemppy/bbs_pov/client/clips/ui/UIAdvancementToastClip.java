package wemppy.bbs_pov.client.clips.ui;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import wemppy.bbs_pov.clips.AdvancementToastClip;

public class UIAdvancementToastClip extends UIClip<AdvancementToastClip>
{
    public UITextbox title;
    public UITextbox description;
    public UIItemStack icon;
    public UITextbox banner;

    public UIAdvancementToastClip(AdvancementToastClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.title = this.textbox(60, this.clip.title);
        this.description = this.textbox(60, this.clip.description);
        this.icon = this.itemStack(this.clip.icon);
        this.banner = this.textbox(100, this.clip.banner);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Toast Content"), this.title, this.description, this.icon));
        this.panels.add(this.section(IKey.raw("Top Banner Message"), this.banner));
    }
}
