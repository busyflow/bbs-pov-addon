package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.List;

/** Bakeable / authored recreation of Game Menu, Death, and Leave Bed screens. */
public final class MenuPovActionClip extends PovActionClip
{
    public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
    public final KeyframeChannel<Transform> cursorLayout = this.channel("cursor_layout", KeyframeFactories.TRANSFORM);
    public final KeyframeChannel<Boolean> cursorVisible = this.channel("cursor_visible", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<String> deathMessage = this.channel("death_message", KeyframeFactories.STRING);
    public final KeyframeChannel<String> score = this.channel("score", KeyframeFactories.STRING);
    public final KeyframeChannel<Float> bgOpacity = this.channel("bg_opacity", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> opacity = this.channel("opacity", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Boolean> buttonsActive = this.channel("buttons_active", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> leaveBed = this.channel("leave_bed", KeyframeFactories.BOOLEAN);

    public MenuPovActionClip()
    {
        super();
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.MENU;
    }

    public String resolveType()
    {
        return this.state.isEmpty() ? "game_menu" : this.state.get(0).getValue();
    }

    @Override
    public void normalize()
    {
        super.normalize();
        clamp(this.bgOpacity, 0F, 1F);
        clamp(this.opacity, 0F, 1F);
    }

    public void ensureBakingBounds()
    {
        float end = this.duration.get();
        this.padChannel(this.state, end);
        this.padChannel(this.cursorLayout, end);
        this.padChannel(this.cursorVisible, end);
        this.padChannel(this.deathMessage, end);
        this.padChannel(this.score, end);
        this.padChannel(this.bgOpacity, end, true);
        this.padChannel(this.opacity, end, false);
        this.padChannel(this.buttonsActive, end, true);
        this.padChannel(this.leaveBed, end, true);
        linear(this.opacity);
    }

    private static <T> void padChannel(KeyframeChannel<T> channel, float end)
    {
        padChannel(channel, end, true);
    }

    private static <T> void padChannel(KeyframeChannel<T> channel, float end, boolean hold)
    {
        if (channel == null || channel.isEmpty() || end <= 0F)
        {
            return;
        }

        List<? extends Keyframe<T>> keyframes = channel.getKeyframes();
        Keyframe<T> first = keyframes.get(0);
        Keyframe<T> last = keyframes.get(keyframes.size() - 1);

        if (first.getTick() > 0F)
        {
            channel.insert(0F, first.getValue());
        }

        if (last.getTick() < end)
        {
            channel.insert(end, last.getValue());
        }

        if (hold)
        {
            constant(channel);
        }
    }

    @Override
    protected Clip create()
    {
        return new MenuPovActionClip();
    }
}
