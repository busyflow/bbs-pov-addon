package Glaxium.POV.actions.gui.data;

/** Extra beacon selected effects and pyramid level. */
public final class BeaconSnapshot
{
    public final int primary;
    public final int secondary;
    public final int level;

    public BeaconSnapshot(int primary, int secondary, int level)
    {
        this.primary = primary;
        this.secondary = secondary;
        this.level = level;
    }
}
