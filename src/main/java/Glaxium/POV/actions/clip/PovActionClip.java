package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for all native BBS-POV Action timeline clips. */
public abstract class PovActionClip extends Clip
{
    private final List<KeyframeChannel<?>> channels = new ArrayList<>();

    public PovActionClip()
    {
        super();
        this.layer.set(this.getActionType().seedLayer());
    }

    public abstract PovActionType getActionType();

    public float getLocalTick(float globalTick)
    {
        return Math.max(0F, globalTick - this.tick.get());
    }

    public boolean isActive(float globalTick)
    {
        if (!this.enabled.get())
        {
            return false;
        }

        int start = this.tick.get();
        int end = start + Math.max(0, this.duration.get());

        return globalTick >= start && globalTick <= end;
    }

    protected final <T> KeyframeChannel<T> channel(String id, IKeyframeFactory<T> factory)
    {
        KeyframeChannel<T> channel = new KeyframeChannel<>(id, factory);

        this.channels.add(channel);
        this.add(channel);

        return channel;
    }

    public final List<KeyframeChannel<?>> getChannels()
    {
        return Collections.unmodifiableList(this.channels);
    }

    public static void constant(KeyframeChannel<?> channel)
    {
        if (channel == null)
        {
            return;
        }

        for (Keyframe<?> keyframe : channel.getKeyframes())
        {
            keyframe.getInterpolation().setInterp(Interpolations.CONST);
        }
    }

    public static void linear(KeyframeChannel<?> channel)
    {
        if (channel == null)
        {
            return;
        }

        for (Keyframe<?> keyframe : channel.getKeyframes())
        {
            keyframe.getInterpolation().setInterp(Interpolations.LINEAR);
        }
    }

    public static void clamp(KeyframeChannel<Float> channel, float minimum, float maximum)
    {
        for (Keyframe<Float> keyframe : channel.getKeyframes())
        {
            keyframe.setValue(Math.max(minimum, Math.min(maximum, keyframe.getValue())));
        }
    }

    public static void clamp(KeyframeChannel<Double> channel, double minimum, double maximum)
    {
        for (Keyframe<Double> keyframe : channel.getKeyframes())
        {
            keyframe.setValue(Math.max(minimum, Math.min(maximum, keyframe.getValue())));
        }
    }

    public static void clamp(KeyframeChannel<Integer> channel, int minimum, int maximum)
    {
        for (Keyframe<Integer> keyframe : channel.getKeyframes())
        {
            keyframe.setValue(Math.max(minimum, Math.min(maximum, keyframe.getValue())));
        }
    }

    public void normalize()
    {
        /* Early POV builds stored the action type as an explicit title.
         * BBS actions leave title empty and show the type as a placeholder. */
        if (this.title.get().equals(this.getActionType().title))
        {
            this.title.set("");
        }

        if (this.duration.get() < 1)
        {
            this.duration.set(1);
        }
    }
}
