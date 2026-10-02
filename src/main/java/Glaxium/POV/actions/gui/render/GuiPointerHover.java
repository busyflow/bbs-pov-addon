package Glaxium.POV.actions.gui.render;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/** Hover target while rendering a GUI clip (item, widget, or tooltip lines). */
public final class GuiPointerHover
{
    public ItemStack item;
    public Text widget;
    public List<Text> lines;
    public boolean itemGroup;
}
