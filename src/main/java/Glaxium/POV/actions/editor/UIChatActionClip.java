package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ChatPovActionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/** Chat Action Inspector panel with Command Recommendations toggle and Keyframe Editor. */
public class UIChatActionClip extends UIPovActionClip<ChatPovActionClip>
{
    public UIToggle showRecommendations;
    public UIButton editKeyframes;
    public UIKeyframeEditor keyframes;

    public UIChatActionClip(ChatPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.duration(() -> this.clip.duration.get());

        this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), (button) ->
        {
            this.updateKeyframeSheets();
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            if (this.keyframes.view.getGraph() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
        });

        this.showRecommendations = new UIToggle(IKey.constant("Command Recommendations"), (toggle) ->
        {
            this.clip.showRecommendations.set(toggle.getValue());
            this.editor.fillData();
        });
    }

    private void updateKeyframeSheets()
    {
        this.keyframes.view.removeAllSheets();
        this.keyframes.view.addSheet(new UIKeyframeSheet(
            "chat_text",
            IKey.constant("Chat Text"),
            0xffffff,
            this.clip.text,
            null).icon(Icons.FONT).seed(() -> ""));
        this.keyframes.view.addSheet(new UIKeyframeSheet(
            "chat_bar_visible",
            IKey.constant("Background Bar / Cursor"),
            0x55ff55,
            this.clip.barVisible,
            null).icon(Icons.LAYOUT).seed(() -> true));
        this.keyframes.view.addSheet(new UIKeyframeSheet(
            "chat_cursor_pos",
            IKey.constant("Cursor Position"),
            0x55ffff,
            this.clip.cursorPos,
            null).icon(Icons.POINTER).seed(() -> 0));
        this.keyframes.view.addSheet(new UIKeyframeSheet(
            "executed_text",
            IKey.constant("Executed Text"),
            0xffaa00,
            this.clip.executedText,
            null).icon(Icons.FONT).seed(() -> ""));
        this.keyframes.view.addSheet(new UIKeyframeSheet(
            "chat_scroll",
            IKey.constant("Chat Scroll"),
            0x55aaff,
            this.clip.chatScroll,
            null).icon(Icons.MORE).seed(() -> 0));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Chat"),
            this.editKeyframes,
            this.showRecommendations));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.clip.createDefaultKeyframes();
        this.showRecommendations.setValue(this.clip.showRecommendations.get());
        this.updateKeyframeSheets();
    }
}
