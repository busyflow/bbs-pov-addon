package wemppy.bbs_pov.clips;

import net.minecraft.item.ItemStack;

public class ToastData
{
    public final String title;
    public final String description;
    public final ItemStack icon;
    public final String banner;
    public final float factor;
    public final float relTick;
    public final int duration;

    public ToastData(String title, String description, ItemStack icon, String banner, float factor, float relTick, int duration)
    {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.banner = banner;
        this.factor = factor;
        this.relTick = relTick;
        this.duration = duration;
    }
}
