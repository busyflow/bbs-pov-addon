package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;

public class UISemanticHudActionClip extends UIPovActionClip<SemanticHudPovActionClip>
{
    public UITrackpad opacity;
    public UITextbox state;

    public UISemanticHudActionClip(SemanticHudPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.opacity = new UITrackpad((value) ->
        {
            this.editor.editMultiple(this.clip.opacity, (channel) ->
            {
                if (channel.isEmpty())
                {
                    channel.insert(0F, value.floatValue());
                }
                else
                {
                    channel.get(0).setValue(value.floatValue());
                }
            });
        });
        this.opacity.limit(0F, 1F, true).forcedLabel(IKey.constant("Opacity"));

        this.state = new UITextbox(10000, (text) ->
        {
            this.editor.editMultiple(this.clip.state, (channel) ->
            {
                if (channel.isEmpty())
                {
                    channel.insert(0F, text);
                }
                else
                {
                    channel.get(0).setValue(text);
                }
            });
        });
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant(this.clip.getActionType().title + " Settings"),
            this.opacity,
            this.state));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.opacity.setValue(this.clip.opacity.isEmpty() ? 1F : this.clip.opacity.get(0).getValue());
        this.state.setText(this.clip.state.isEmpty() ? "" : this.clip.state.get(0).getValue());
    }
}
