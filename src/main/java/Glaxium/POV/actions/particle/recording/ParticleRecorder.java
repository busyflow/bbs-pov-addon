package Glaxium.POV.actions.particle.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public final class ParticleRecorder {
   private static final String STATUS_KEY = "status:entity_effect";
   private static final int BURST_GRACE = 2;
   private static final double PLAYER_RANGE = 4.5;
   private static ParticleRecorder current;
   private static int captureSuspended;
   private final List<ParticleRecorder.Spawn> pending = new ArrayList<>();
   private ParticleRecorder.OpenEffect openStatus;
   private ParticleRecorder.OpenEffect openBurst;
   private ReplayKeyframesPovAccess access;
   private Recorder recorder;

   public void reset() {
      this.pending.clear();
      this.openStatus = null;
      this.openBurst = null;
      this.access = null;
      this.recorder = null;
      if (current == this) {
         current = null;
      }
   }

   public void arm() {
      current = this;
   }

   public void finish(int tick) {
      this.commitBursts(tick, null);
      if (this.openBurst != null) {
         this.openBurst.clip.duration.set(Math.max(1, this.openBurst.lastTick - (Integer)this.openBurst.clip.tick.get() + 1));
         this.openBurst = null;
      }

      if (this.openStatus != null) {
         this.openStatus.clip.duration.set(Math.max(1, this.openStatus.lastTick - (Integer)this.openStatus.clip.tick.get() + 1));
         this.openStatus = null;
      }

      if (current == this) {
         current = null;
      }
   }

   public void record(ReplayKeyframesPovAccess access, Recorder recorder, ClientPlayerEntity player) {
      if (!PovSettings.isBakeParticles()) {
         this.pending.clear();
      } else {
         this.access = access;
         this.recorder = recorder;
         if (!recorder.hasNotStarted() && recorder.tick >= 0) {
            current = this;
            this.recordStatus(recorder.tick, player);
            this.commitBursts(recorder.tick, player);
         } else {
            this.pending.clear();
         }
      }
   }

   public static boolean isArmed() {
      return current != null;
   }

   public static boolean isPlaybackEmit() {
      return captureSuspended > 0;
   }

   public static void suspendCapture() {
      captureSuspended++;
   }

   public static void resumeCapture() {
      captureSuspended = Math.max(0, captureSuspended - 1);
   }

   public static void capture(ParticleEffect parameters, double x, double y, double z) {
      if (captureSuspended <= 0) {
         ParticleRecorder recorder = current;
         if (recorder != null && PovSettings.isBakeParticles() && parameters != null) {
            ParticleType<?> type = parameters.getType();
            if (type != ParticleTypes.ENTITY_EFFECT
               && type != ParticleTypes.AMBIENT_ENTITY_EFFECT
               && type != ParticleTypes.EFFECT
               && type != ParticleTypes.TOTEM_OF_UNDYING
               && isBurstEffect(type)) {
               recorder.pending.add(new ParticleRecorder.Spawn(parameters, x, y, z));
            }
         }
      }
   }

   private void recordStatus(int tick, ClientPlayerEntity player) {
      if (this.access != null && player != null) {
         int color = visibleStatusColor(player);
         if (color >= 0) {
            if (this.openStatus == null) {
               ParticleEffectPovActionClip clip = (ParticleEffectPovActionClip)this.access.bbsPov$getActions().add(PovActionType.PARTICLE_EFFECT, tick, 1);
               clip.particle.set(Registries.PARTICLE_TYPE.getId(ParticleTypes.ENTITY_EFFECT).toString());
               clip.dustR.set((float)(color >> 16 & 0xFF) / 255.0F);
               clip.dustG.set((float)(color >> 8 & 0xFF) / 255.0F);
               clip.dustB.set((float)(color & 0xFF) / 255.0F);
               this.openStatus = new ParticleRecorder.OpenEffect(clip, tick);
            } else {
               this.openStatus.clip.duration.set(Math.max(1, tick - (Integer)this.openStatus.clip.tick.get() + 1));
               this.openStatus.lastTick = tick;
            }
         } else if (this.openStatus != null) {
            this.openStatus.clip.duration.set(Math.max(1, this.openStatus.lastTick - (Integer)this.openStatus.clip.tick.get() + 1));
            this.openStatus = null;
         }
      }
   }

   private void commitBursts(int tick, ClientPlayerEntity player) {
      if (this.access != null && this.recorder != null) {
         ParticleRecorder.Spawn validSpawn = null;

         for (ParticleRecorder.Spawn spawn : this.pending) {
            if (player == null || player.squaredDistanceTo(spawn.x, spawn.y, spawn.z) <= 20.25) {
               validSpawn = spawn;
               break;
            }
         }

         this.pending.clear();
         if (validSpawn != null) {
            if (this.openBurst == null) {
               String id = Registries.PARTICLE_TYPE.getId(validSpawn.parameters.getType()).toString();
               ParticleEffectPovActionClip clip = (ParticleEffectPovActionClip)this.access.bbsPov$getActions().add(PovActionType.PARTICLE_EFFECT, tick, 1);
               clip.particle.set(id);
               fillExtra(clip, validSpawn.parameters);
               this.openBurst = new ParticleRecorder.OpenEffect(clip, tick);
            } else {
               this.openBurst.clip.duration.set(Math.max(1, tick - (Integer)this.openBurst.clip.tick.get() + 1));
               this.openBurst.lastTick = tick;
            }
         } else if (this.openBurst != null && tick - this.openBurst.lastTick > 2) {
            this.openBurst.clip.duration.set(Math.max(1, this.openBurst.lastTick - (Integer)this.openBurst.clip.tick.get() + 1));
            this.openBurst = null;
         }
      } else {
         this.pending.clear();
      }
   }

   private static String effectKey(ParticleEffect effect) {
      String id = Registries.PARTICLE_TYPE.getId(effect.getType()).toString();
      if (effect instanceof ItemStackParticleEffect item && !item.getItemStack().isEmpty() && !item.getItemStack().isOf(Items.AIR)) {
         return id + ":" + Registries.ITEM.getId(item.getItemStack().getItem());
      }

      return effect instanceof BlockStateParticleEffect block ? id + ":" + Registries.BLOCK.getId(block.getBlockState().getBlock()) : id;
   }

   private static int visibleStatusColor(ClientPlayerEntity player) {
      int color = -1;

      for (StatusEffectInstance instance : player.getStatusEffects()) {
         if (instance != null && instance.shouldShowParticles()) {
            color = instance.getEffectType().getColor();
         }
      }

      return color;
   }

   private static boolean isBurstEffect(ParticleType<?> type) {
      return type == ParticleTypes.BLOCK
         || type == ParticleTypes.BLOCK_MARKER
         || type == ParticleTypes.FALLING_DUST
         || type == ParticleTypes.CRIT
         || type == ParticleTypes.ENCHANTED_HIT
         || type == ParticleTypes.SWEEP_ATTACK
         || type == ParticleTypes.DAMAGE_INDICATOR
         || type == ParticleTypes.HEART
         || type == ParticleTypes.NOTE
         || type == ParticleTypes.SOUL
         || type == ParticleTypes.WITCH
         || type == ParticleTypes.INSTANT_EFFECT
         || type == ParticleTypes.LAVA
         || type == ParticleTypes.LANDING_LAVA
         || type == ParticleTypes.DRIPPING_LAVA
         || type == ParticleTypes.SPLASH
         || type == ParticleTypes.BUBBLE_POP
         || type == ParticleTypes.DRIPPING_WATER
         || type == ParticleTypes.SMOKE
         || type == ParticleTypes.LARGE_SMOKE
         || type == ParticleTypes.FLAME
         || type == ParticleTypes.SMALL_FLAME
         || type == ParticleTypes.EXPLOSION
         || type == ParticleTypes.EXPLOSION_EMITTER
         || type == ParticleTypes.ANGRY_VILLAGER
         || type == ParticleTypes.HAPPY_VILLAGER
         || type == ParticleTypes.POOF
         || type == ParticleTypes.FIREWORK
         || type == ParticleTypes.FLASH;
   }

   private static void fillExtra(ParticleEffectPovActionClip clip, ParticleEffect parameters) {
      if (parameters instanceof BlockStateParticleEffect block) {
         Identifier blockId = Registries.BLOCK.getId(block.getBlockState().getBlock());
         clip.blockId.set(blockId.toString());
      } else if (parameters instanceof ItemStackParticleEffect item) {
         if (!item.getItemStack().isEmpty() && !item.getItemStack().isOf(Items.AIR)) {
            clip.itemId.set(Registries.ITEM.getId(item.getItemStack().getItem()).toString());
         }
      } else if (parameters instanceof DustParticleEffect dust) {
         Vector3f color = dust.getColor();
         clip.dustR.set(color.x());
         clip.dustG.set(color.y());
         clip.dustB.set(color.z());
         clip.dustScale.set(dust.getScale());
      }
   }

   private static final class OpenEffect {
      final ParticleEffectPovActionClip clip;
      int lastTick;

      OpenEffect(ParticleEffectPovActionClip clip, int tick) {
         this.clip = clip;
         this.lastTick = tick;
      }
   }

   private static final class Spawn {
      final ParticleEffect parameters;
      final double x;
      final double y;
      final double z;

      Spawn(ParticleEffect parameters, double x, double y, double z) {
         this.parameters = parameters;
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
