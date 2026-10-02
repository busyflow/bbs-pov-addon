package wemppy.bbs_pov.clips;

import net.minecraft.item.ItemStack;

public class ScreenEffectsData
{
    public final String effectType;
    public final ItemStack item;
    public final float progress;
    public final float relTick;
    public final int duration;
    public final float factor;
    public final float scale;
    public final boolean showParticles;
    public final boolean playSound;
    public final int absoluteTicks;

    public ScreenEffectsData(String effectType, ItemStack item, float progress, float relTick, int duration, float factor, float scale, boolean showParticles, boolean playSound, int absoluteTicks)
    {
        this.effectType = effectType;
        this.item = item;
        this.progress = progress;
        this.relTick = relTick;
        this.duration = duration;
        this.factor = factor;
        this.scale = scale;
        this.showParticles = showParticles;
        this.playSound = playSound;
        this.absoluteTicks = absoluteTicks;
    }
}
