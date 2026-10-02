package Glaxium.POV.actions.gui.data;

/** Extra furnace lit and cook progress. */
public final class FurnaceSnapshot
{
    public final float lit;
    public final float cook;

    public FurnaceSnapshot(float lit, float cook)
    {
        this.lit = lit;
        this.cook = cook;
    }
}
