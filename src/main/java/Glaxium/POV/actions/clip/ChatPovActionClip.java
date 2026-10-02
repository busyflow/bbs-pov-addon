package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/** POV Action Clip managing Chat screen HUD playback and keyframed text / suggestions. */
public class ChatPovActionClip extends PovActionClip
{
    public final KeyframeChannel<String> text = this.channel("chat_text", KeyframeFactories.STRING);
    public final KeyframeChannel<Boolean> barVisible = this.channel("chat_bar_visible", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Integer> cursorPos = this.channel("chat_cursor_pos", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Integer> selStart = this.channel("chat_sel_start", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Integer> selEnd = this.channel("chat_sel_end", KeyframeFactories.INTEGER);
    public final KeyframeChannel<String> executedText = this.channel("executed_text", KeyframeFactories.STRING);
    public final KeyframeChannel<Integer> chatScroll = this.channel("chat_scroll", KeyframeFactories.INTEGER);
    public final ValueBoolean showRecommendations = new ValueBoolean("show_recommendations", true);

    public ChatPovActionClip()
    {
        super();
        this.add(this.showRecommendations);
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.CHAT;
    }

    public void createDefaultKeyframes()
    {
        if (this.text.isEmpty())
        {
            this.text.insert(0F, "");
        }
        if (this.barVisible.isEmpty())
        {
            this.barVisible.insert(0F, true);
        }
        if (this.cursorPos.isEmpty())
        {
            this.cursorPos.insert(0F, 0);
        }
        if (this.selStart.isEmpty())
        {
            this.selStart.insert(0F, -1);
        }
        if (this.selEnd.isEmpty())
        {
            this.selEnd.insert(0F, -1);
        }
        if (this.chatScroll.isEmpty())
        {
            this.chatScroll.insert(0F, 0);
        }
    }

    @Override
    protected Clip create()
    {
        ChatPovActionClip clip = new ChatPovActionClip();
        clip.copy(this);
        return clip;
    }
}
