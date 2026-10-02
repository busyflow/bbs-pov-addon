package Glaxium.POV.actions.gui.data;

/** Extra horse variant or donkey chest-open flag. */
public final class MountSnapshot
{
    public final int horseVariant;
    public final boolean chestOpen;

    public MountSnapshot(int horseVariant, boolean chestOpen)
    {
        this.horseVariant = horseVariant;
        this.chestOpen = chestOpen;
    }
}
