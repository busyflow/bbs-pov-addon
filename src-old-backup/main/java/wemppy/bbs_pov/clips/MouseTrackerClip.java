package wemppy.bbs_pov.clips;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;

public class MouseTrackerClip extends CameraClip
{
    public final ValueFloat x = new ValueFloat("x", 0.5F, 0F, 1F);
    public final ValueFloat y = new ValueFloat("y", 0.5F, 0F, 1F);
    public final ValueBoolean visible = new ValueBoolean("visible", true);
    public final ValueBoolean clicking = new ValueBoolean("clicking", false);
    public final ValueBoolean liveTracking = new ValueBoolean("live_tracking", false);

    public final KeyframeChannel<Double> xChannel = new KeyframeChannel<>("x_channel", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> yChannel = new KeyframeChannel<>("y_channel", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> visibleChannel = new KeyframeChannel<>("visible_channel", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> clickingChannel = new KeyframeChannel<>("clicking_channel", KeyframeFactories.BOOLEAN);

    public MouseTrackerClip()
    {
        super();

        this.add(this.x);
        this.add(this.y);
        this.add(this.visible);
        this.add(this.clicking);
        this.add(this.liveTracking);

        this.add(this.xChannel);
        this.add(this.yChannel);
        this.add(this.visibleChannel);
        this.add(this.clickingChannel);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float relTick = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), relTick);

        if (factor <= 0F)
        {
            return;
        }

        double posX = !this.xChannel.isEmpty() ? this.xChannel.interpolate(relTick) : this.x.get().doubleValue();
        double posY = !this.yChannel.isEmpty() ? this.yChannel.interpolate(relTick) : this.y.get().doubleValue();
        boolean vis = !this.visibleChannel.isEmpty() ? this.visibleChannel.interpolate(relTick) : this.visible.get();
        boolean click = !this.clickingChannel.isEmpty() ? this.clickingChannel.interpolate(relTick) : this.clicking.get();

        if (vis)
        {
            MouseTrackerData data = new MouseTrackerData((float) posX, (float) posY, click, factor);
            context.clipData.get("bbs_mouse_tracker", ArrayList::new).add(data);
        }
    }

    @Override
    protected Clip create()
    {
        return new MouseTrackerClip();
    }
}
