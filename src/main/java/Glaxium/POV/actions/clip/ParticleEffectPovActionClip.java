package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ParticleEffectPovActionClip extends ViewParticlePovActionClip
{
    public final ValueString particle = new ValueString("particle", "minecraft:poof");
    public final ValueString blockId = new ValueString("block", "minecraft:stone");
    public final ValueString itemId = new ValueString("item", "minecraft:apple");
    public final ValueFloat dustR = new ValueFloat("dust_r", 1F, 0F, 1F);
    public final ValueFloat dustG = new ValueFloat("dust_g", 0F, 0F, 1F);
    public final ValueFloat dustB = new ValueFloat("dust_b", 0F, 0F, 1F);
    public final ValueFloat dustScale = new ValueFloat("dust_scale", 1F, 0.01F, 4F);

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
