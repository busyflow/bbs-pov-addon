package Glaxium.POV.actions.gui.data;

/** Extra stonecutter recipe-list scroll row. */
public final class StonecutterSnapshot
{
    public final int row;

    public StonecutterSnapshot(int row)
    {
        this.row = Math.max(0, row);
    }
}
