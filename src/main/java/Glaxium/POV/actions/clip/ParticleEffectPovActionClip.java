package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ParticleEffectPovActionClip extends ViewParticlePovActionClip
{
    public static final String CUSTOM_COLORED_EFFECT_ID = "bbs_pov:custom_colored_effect";
    public static final String CUSTOM_COLORED_EFFECT_NAME = "Custom Colored Effect";
    /** Vanilla Minecraft Poison potion swirl RGB color. */
    public static final int DEFAULT_CUSTOM_EFFECT_COLOR = 0x4E9331;

    public final ValueString particle = new ValueString("particle", "minecraft:poof");
    public final ValueString blockId = new ValueString("block", "minecraft:stone");
    public final ValueString itemId = new ValueString("item", "minecraft:apple");
    public final ValueFloat dustR = new ValueFloat("dust_r", 1F, 0F, 1F);
    public final ValueFloat dustG = new ValueFloat("dust_g", 0F, 0F, 1F);
    public final ValueFloat dustB = new ValueFloat("dust_b", 0F, 0F, 1F);
    public final ValueFloat dustScale = new ValueFloat("dust_scale", 1F, 0.01F, 4F);
    public final ValueBoolean rainbow = new ValueBoolean("rainbow", false);
    public final ValueInt color = new ValueInt("color", -1);

    public ParticleEffectPovActionClip()
    {
        super();
        this.add(this.particle);
        this.add(this.blockId);
        this.add(this.itemId);
        this.add(this.dustR);
        this.add(this.dustG);
        this.add(this.dustB);
        this.add(this.dustScale);
        this.add(this.rainbow);
        this.add(this.color);
    }

    public boolean isCustomColoredEffect()
    {
        return CUSTOM_COLORED_EFFECT_ID.equals(this.particle.get());
    }

    public int getColorRgb()
    {
        int custom = this.color.get();

        if (custom >= 0)
        {
            return custom & 0xFFFFFF;
        }

        if (this.isCustomColoredEffect())
        {
            return DEFAULT_CUSTOM_EFFECT_COLOR;
        }

        int r = Math.max(0, Math.min(255, Math.round(this.dustR.get() * 255F)));
        int g = Math.max(0, Math.min(255, Math.round(this.dustG.get() * 255F)));
        int b = Math.max(0, Math.min(255, Math.round(this.dustB.get() * 255F)));

        return (r << 16) | (g << 8) | b;
    }

    public ItemStack extraItem()
    {
        return parseItem(this.itemId.get(), Items.APPLE);
    }

    static ItemStack parseItem(String id, net.minecraft.item.Item fallback)
    {
        net.minecraft.util.Identifier identifier = net.minecraft.util.Identifier.tryParse(id);
        net.minecraft.item.Item item = identifier == null
            ? fallback
            : net.minecraft.registry.Registries.ITEM.get(identifier);

        if (item == null || item == net.minecraft.item.Items.AIR)
        {
            item = fallback != null && fallback != net.minecraft.item.Items.AIR ? fallback : net.minecraft.item.Items.APPLE;
        }

        return new ItemStack(item);
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.PARTICLE_EFFECT;
    }

    @Override
    protected Clip create()
    {
        return new ParticleEffectPovActionClip();
    }
}
