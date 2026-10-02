package Glaxium.POV.actions.gui.data;

/** Extra anvil name-field and error-arrow state. */
public final class AnvilSnapshot
{
    public final String name;
    public final boolean focused;
    public final int selStart;
    public final int selEnd;
    public final boolean error;

    public AnvilSnapshot(String name, boolean focused, int selStart, int selEnd, boolean error)
    {
        this.name = name == null ? "" : name;
        this.focused = focused;
        this.selStart = Math.max(0, selStart);
        this.selEnd = Math.max(0, selEnd);
        this.error = error;
    }
}
