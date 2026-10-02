package Glaxium.POV.actions.gui.data;

/** Extra loom pattern-grid scroll row. */
public final class LoomSnapshot
{
    public final int row;

    public LoomSnapshot(int row)
    {
        this.row = Math.max(0, row);
    }
}
