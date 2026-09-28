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

final class ParticleEffects {
   private ParticleEffects() {
   }

   static ParticleEffect fromClip(ParticleEffectPovActionClip clip) {
      Identifier id = Identifier.tryParse((String)clip.particle.get());
      ParticleType<?> type = id == null ? null : (ParticleType)Registries.PARTICLE_TYPE.get(id);
      if (type == null) {
         return ParticleTypes.POOF;
      } else if (type instanceof DefaultParticleType) {
         return (DefaultParticleType)type;
      } else if (type == ParticleTypes.BLOCK || type == ParticleTypes.BLOCK_MARKER || type == ParticleTypes.FALLING_DUST) {
         return new BlockStateParticleEffect(type, resolveBlock((String)clip.blockId.get()).getDefaultState());
      } else if (type == ParticleTypes.ITEM) {
         return new ItemStackParticleEffect(ParticleTypes.ITEM, clip.extraItem());
      } else {
         return (ParticleEffect)(type == ParticleTypes.DUST
            ? new DustParticleEffect(new Vector3f((Float)clip.dustR.get(), (Float)clip.dustG.get(), (Float)clip.dustB.get()), (Float)clip.dustScale.get())
            : ParticleTypes.POOF);
      }
   }

   private static Block resolveBlock(String id) {
      Identifier identifier = Identifier.tryParse(id);
      Block block = identifier == null ? Blocks.STONE : (Block)Registries.BLOCK.get(identifier);
      return block == null ? Blocks.STONE : block;
   }
}
