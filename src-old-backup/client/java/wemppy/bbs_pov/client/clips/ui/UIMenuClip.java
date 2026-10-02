package wemppy.bbs_pov.client.clips.ui;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import wemppy.bbs_pov.clips.MenuClip;

public class UIMenuClip extends UIClip<MenuClip>
{
    public UIButton menuTypeButton;
    public UIButton editKeyframes;
    public UIKeyframeEditor keyframes;

    public UITextbox deathMessage;
    public UITextbox score;
    public UIColor redBackground;
    public UIToggle buttonsActive;
    public UIToggle hardcore;

    public UIMenuClip(MenuClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            if (this.editor instanceof UIClipsPanel panel && this.clip.getParent() instanceof Clips clips)
            {
                UIReplaysEditor.renderRuler(context, this.keyframes.view, panel, clips, this.clip.tick.get());
            }
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("menu_keyframes");

        UIKeyframeSheet sheetDeath = new UIKeyframeSheet("death_message", IKey.raw("Death Message"), Colors.WHITE, this.clip.deathMessageChannel, this.clip.deathMessage).icon(Icons.FONT);
        UIKeyframeSheet sheetScore = new UIKeyframeSheet("score", IKey.raw("Score"), Colors.YELLOW, this.clip.scoreChannel, this.clip.score).icon(Icons.FONT);
        UIKeyframeSheet sheetRed = new UIKeyframeSheet("red_background", IKey.raw("Red Background"), Colors.RED, this.clip.redBackgroundChannel, null).icon(Icons.COLOR);
        UIKeyframeSheet sheetButtons = new UIKeyframeSheet("buttons_active", IKey.raw("Buttons Active"), Colors.ACTIVE, this.clip.buttonsActiveChannel, this.clip.buttonsActive).icon(Icons.POINTER);

        this.keyframes.view.addSheet(sheetDeath);
        this.keyframes.view.addSheet(sheetScore);
        this.keyframes.view.addSheet(sheetRed);
        this.keyframes.view.addSheet(sheetButtons);

        this.menuTypeButton = new UIButton(IKey.raw("Menu: Death"), (b) -> {});

        this.editKeyframes = new UIButton(IKey.raw("Edit Keyframes"), (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.editKeyframes.keys().register(Keys.FORMS_EDIT, () -> this.editKeyframes.clickItself());

        this.deathMessage = this.textbox(100, this.clip.deathMessage);
        this.score = this.textbox(50, this.clip.score);
        this.redBackground = this.color(this.clip.redBackground);
        this.buttonsActive = this.toggle(IKey.raw("Buttons Active"), this.clip.buttonsActive);
        this.hardcore = this.toggle(IKey.raw("Hardcore (Game Over)"), this.clip.hardcore);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Menu Settings"), this.menuTypeButton));
        this.panels.add(this.section(IKey.raw("Menu Keyframes"), this.editKeyframes));
        this.panels.add(this.section(IKey.raw("Death Screen Options"), this.deathMessage, this.score, this.redBackground, this.buttonsActive, this.hardcore));
    }
}
