package wemppy.bbs_pov.clips;

public class MouseTrackerData
{
    public final float x;
    public final float y;
    public final boolean clicking;
    public final float factor;

    public MouseTrackerData(float x, float y, boolean clicking, float factor)
    {
        this.x = x;
        this.y = y;
        this.clicking = clicking;
        this.factor = factor;
    }
}
