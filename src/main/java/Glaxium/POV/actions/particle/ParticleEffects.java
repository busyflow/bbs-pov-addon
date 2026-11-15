package Glaxium.POV.actions.particle;

import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public final class ParticleEffects
{
    private static final long RAINBOW_PERIOD_TICKS = 60L;

    private ParticleEffects()
    {
    }

    public static boolean supportsCustomColor(ParticleEffectPovActionClip clip)
    {
        if (clip == null)
        {
            return false;
        }

        if (clip.isCustomColoredEffect())
        {
            return true;
        }

        String id = clip.particle.get();

        return "minecraft:entity_effect".equals(id)
            || "minecraft:ambient_entity_effect".equals(id)
            || "minecraft:dust".equals(id);
    }

    public static String displayName(String particleId)
    {
        if (ParticleEffectPovActionClip.CUSTOM_COLORED_EFFECT_ID.equals(particleId))
        {
            return ParticleEffectPovActionClip.CUSTOM_COLORED_EFFECT_NAME;
        }

        return particleId == null || particleId.isEmpty() ? "minecraft:poof" : particleId;
    }

    public static String fromDisplayName(String value)
    {
        if (ParticleEffectPovActionClip.CUSTOM_COLORED_EFFECT_NAME.equals(value))
        {
            return ParticleEffectPovActionClip.CUSTOM_COLORED_EFFECT_ID;
        }

        return value;
    }

    public static float[] resolveColorRgb(ParticleEffectPovActionClip clip, long tickTime)
    {
        if (clip != null && clip.rainbow.get())
        {
            long step = ((tickTime % RAINBOW_PERIOD_TICKS) + RAINBOW_PERIOD_TICKS) % RAINBOW_PERIOD_TICKS;
            float hue = step / (float) RAINBOW_PERIOD_TICKS;

            return hsvToRgb(hue, 0.85F, 1.0F);
        }

        int rgb = clip == null ? ParticleEffectPovActionClip.DEFAULT_CUSTOM_EFFECT_COLOR : clip.getColorRgb();

        return new float[] {
            ((rgb >> 16) & 0xFF) / 255F,
            ((rgb >> 8) & 0xFF) / 255F,
            (rgb & 0xFF) / 255F
        };
    }

    private static float[] hsvToRgb(float hue, float saturation, float value)
    {
        float h = (hue - (float) Math.floor(hue)) * 6F;
        int sector = ((int) Math.floor(h)) % 6;
        float f = h - (float) Math.floor(h);
        float p = value * (1F - saturation);
        float q = value * (1F - f * saturation);
        float t = value * (1F - (1F - f) * saturation);

        return switch (sector)
        {
            case 0 -> new float[] {value, t, p};
            case 1 -> new float[] {q, value, p};
            case 2 -> new float[] {p, value, t};
            case 3 -> new float[] {p, q, value};
            case 4 -> new float[] {t, p, value};
            default -> new float[] {value, p, q};
        };
    }

    static ParticleEffect fromClip(ParticleEffectPovActionClip clip)
    {
        return fromClip(clip, 0L);
    }

    static ParticleEffect fromClip(ParticleEffectPovActionClip clip, long tickTime)
    {
        if (clip.isCustomColoredEffect())
        {
            return ParticleTypes.ENTITY_EFFECT;
        }

        Identifier id = Identifier.tryParse(clip.particle.get());
        ParticleType<?> type = id == null ? null : Registries.PARTICLE_TYPE.get(id);

        if (type == null)
        {
            return ParticleTypes.POOF;
        }

        if (type instanceof DefaultParticleType simple)
        {
            return simple;
        }

        if (type == ParticleTypes.BLOCK || type == ParticleTypes.BLOCK_MARKER || type == ParticleTypes.FALLING_DUST)
        {
            @SuppressWarnings("unchecked")
            ParticleType<BlockStateParticleEffect> blockType = (ParticleType<BlockStateParticleEffect>) type;

            return new BlockStateParticleEffect(blockType, resolveBlock(clip.blockId.get()).getDefaultState());
        }

        if (type == ParticleTypes.ITEM)
        {
            return new ItemStackParticleEffect(ParticleTypes.ITEM, clip.extraItem());
        }

        if (type == ParticleTypes.DUST)
        {
            float[] rgb = resolveColorRgb(clip, tickTime);

            return new DustParticleEffect(
                new Vector3f(rgb[0], rgb[1], rgb[2]),
                clip.dustScale.get());
        }

        return ParticleTypes.POOF;
    }

    private static Block resolveBlock(String id)
    {
        Identifier identifier = Identifier.tryParse(id);
        Block block = identifier == null ? Blocks.STONE : Registries.BLOCK.get(identifier);

        return block == null ? Blocks.STONE : block;
    }
}
