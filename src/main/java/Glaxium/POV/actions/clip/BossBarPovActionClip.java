package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.bossbar.BossBarTypeEntry;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;

/** One vanilla-style boss bar: Dragon, Wither, or Raid. */
public final class BossBarPovActionClip extends PovActionClip
{
    public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
    public final KeyframeChannel<String> name = this.channel("name", KeyframeFactories.STRING);
    public final KeyframeChannel<Float> percent = this.channel("percent", KeyframeFactories.FLOAT);
    public final KeyframeChannel<String> color = this.channel("color", KeyframeFactories.STRING);
    public final KeyframeChannel<String> style = this.channel("style", KeyframeFactories.STRING);

    public BossBarPovActionClip()
    {
        super();
        this.ensureDefaults();
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.BOSS_BARS;
    }

    public String resolveType()
    {
        BossBarTypeEntry entry = BossBarTypeEntry.findById(
            this.state.isEmpty() ? "" : this.state.get(0).getValue());
        return entry == null ? BossBarTypeEntry.DRAGON.id : entry.id;
    }

    public void applyTypeDefaults(BossBarTypeEntry type)
    {
        if (type == null)
        {
            type = BossBarTypeEntry.DRAGON;
        }

        setConstant(this.state, type.id);
        setConstant(this.name, type.defaultTitle);
        setConstant(this.color, type.defaultColor);
        setConstant(this.style, type.defaultStyle);
        if (this.percent.isEmpty())
        {
            this.percent.insert(0F, 1F);
        }
    }

    public void ensureDefaults()
    {
        if (this.state.isEmpty())
        {
            this.applyTypeDefaults(BossBarTypeEntry.DRAGON);
        }
        if (this.name.isEmpty())
        {
            this.name.insert(0F, BossBarTypeEntry.DRAGON.defaultTitle);
        }
        if (this.percent.isEmpty())
        {
            this.percent.insert(0F, 1F);
        }
        if (this.color.isEmpty())
        {
            this.color.insert(0F, BossBarTypeEntry.DRAGON.defaultColor);
        }
        if (this.style.isEmpty())
        {
            this.style.insert(0F, BossBarTypeEntry.DRAGON.defaultStyle);
        }
    }

    @Override
    public void normalize()
    {
        super.normalize();
        this.ensureDefaults();
        clamp(this.percent, 0F, 1F);
        for (Keyframe<String> keyframe : this.color.getKeyframes())
        {
            keyframe.setValue(BossBarLooks.COLORS[BossBarLooks.colorIndex(keyframe.getValue())]);
        }
        for (Keyframe<String> keyframe : this.style.getKeyframes())
        {
            keyframe.setValue(BossBarLooks.STYLES[BossBarLooks.styleIndex(keyframe.getValue())]);
        }
    }

    public void ensureBakingBounds()
    {
        float end = this.duration.get();
        this.padChannel(this.state, end, true);
        this.padChannel(this.name, end, true);
        this.padChannel(this.percent, end, false);
        this.padChannel(this.color, end, true);
        this.padChannel(this.style, end, true);
        linear(this.percent);
        constant(this.state);
        constant(this.name);
        constant(this.color);
        constant(this.style);
    }

    private static <T> void setConstant(KeyframeChannel<T> channel, T value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0F, value);
            return;
        }

        channel.get(0).setValue(value);
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
        return new BossBarPovActionClip();
    }
}
