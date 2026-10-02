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

final class ParticleEffects
{
    private ParticleEffects()
    {
    }

    static ParticleEffect fromClip(ParticleEffectPovActionClip clip)
    {
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
            return new DustParticleEffect(
                new Vector3f(clip.dustR.get(), clip.dustG.get(), clip.dustB.get()),
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
