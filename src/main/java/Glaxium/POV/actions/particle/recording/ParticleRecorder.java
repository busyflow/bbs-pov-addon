package Glaxium.POV.actions.particle.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Bakes player-local particle effects onto the actor.
 * Enforces that only one particle action clip is baked at any given time.
 * Automatically bakes running/sprint block particles based on the ground block beneath the player.
 */
public final class ParticleRecorder
{
    private static final int BURST_GRACE = 2;
    private static final double PLAYER_RANGE = 4.5;

    private static ParticleRecorder current;
    private static int captureSuspended;

    private final List<Spawn> pending = new ArrayList<>();
    private OpenEffect openClip;
    private ReplayKeyframesPovAccess access;
    private Recorder recorder;

    public void reset()
    {
        this.pending.clear();
        this.openClip = null;
        this.access = null;
        this.recorder = null;
        if (current == this)
        {
            current = null;
        }
    }

    public void arm()
    {
        current = this;
    }

    public void finish(int tick)
    {
        if (this.openClip != null)
        {
            this.openClip.clip.duration.set(Math.max(1, this.openClip.lastTick - this.openClip.clip.tick.get() + 1));
            this.openClip = null;
        }
        this.pending.clear();
        if (current == this)
        {
            current = null;
        }
    }

    public void record(ReplayKeyframesPovAccess access, Recorder recorder, ClientPlayerEntity player)
    {
        if (!PovSettings.isBakeParticles())
        {
            this.pending.clear();
            return;
        }

        this.access = access;
        this.recorder = recorder;

        if (recorder.hasNotStarted() || recorder.tick < 0)
        {
            this.pending.clear();
            return;
        }

        current = this;
        int tick = recorder.tick;

        // 1. Priority: Check captured burst particles (hits, crits, sweeps, explosions, etc.)
        Spawn validSpawn = null;
        for (Spawn spawn : this.pending)
        {
            if (player == null || player.squaredDistanceTo(spawn.x, spawn.y, spawn.z) <= PLAYER_RANGE * PLAYER_RANGE)
            {
                validSpawn = spawn;
                break;
            }
        }
        this.pending.clear();

        if (validSpawn != null)
        {
            final Spawn finalSpawn = validSpawn;
            String effectKey = effectKey(finalSpawn.parameters);
            this.activateEffect(tick, effectKey, clip -> {
                String id = Registries.PARTICLE_TYPE.getId(finalSpawn.parameters.getType()).toString();
                clip.particle.set(id);
                fillExtra(clip, finalSpawn.parameters);
            });
            return;
        }

        // 2. Priority: Sprinting / running particles on the ground block
        if (player != null && player.isSprinting() && player.isOnGround())
        {
            BlockState stepping = player.getSteppingBlockState();
            if (stepping != null && !stepping.isAir())
            {
                Identifier blockId = Registries.BLOCK.getId(stepping.getBlock());
                String effectKey = "sprint:block:" + blockId;
                this.activateEffect(tick, effectKey, clip -> {
                    clip.particle.set(Registries.PARTICLE_TYPE.getId(ParticleTypes.BLOCK).toString());
                    clip.blockId.set(blockId.toString());
                });
                return;
            }
        }

        // 3. Priority: Status potion effects
        if (player != null)
        {
            int color = visibleStatusColor(player);
            if (color >= 0)
            {
                String effectKey = "status:entity_effect:" + color;
                this.activateEffect(tick, effectKey, clip -> {
                    clip.particle.set(Registries.PARTICLE_TYPE.getId(ParticleTypes.ENTITY_EFFECT).toString());
                    clip.dustR.set(((color >> 16) & 255) / 255F);
                    clip.dustG.set(((color >> 8) & 255) / 255F);
                    clip.dustB.set((color & 255) / 255F);
                });
                return;
            }
        }

        // 4. No active particle this tick
        if (this.openClip != null)
        {
            if (tick - this.openClip.lastTick > BURST_GRACE)
            {
                this.openClip.clip.duration.set(Math.max(1, this.openClip.lastTick - this.openClip.clip.tick.get() + 1));
                this.openClip = null;
            }
        }
    }

    private void activateEffect(int tick, String key, java.util.function.Consumer<ParticleEffectPovActionClip> configurator)
    {
        if (this.access == null)
        {
            return;
        }

        if (this.openClip != null)
        {
            if (this.openClip.key.equals(key))
            {
                // Continue existing single clip
                this.openClip.clip.duration.set(Math.max(1, tick - this.openClip.clip.tick.get() + 1));
                this.openClip.lastTick = tick;
                return;
            }
            else
            {
                // Close previous clip before opening new one
                this.openClip.clip.duration.set(Math.max(1, this.openClip.lastTick - this.openClip.clip.tick.get() + 1));
                this.openClip = null;
            }
        }

        // Open exactly one active clip
        ParticleEffectPovActionClip clip = (ParticleEffectPovActionClip) this.access.bbsPov$getActions()
            .add(PovActionType.PARTICLE_EFFECT, tick, 1);
        configurator.accept(clip);
        this.openClip = new OpenEffect(clip, key, tick);
    }

    public static boolean isArmed()
    {
        return current != null;
    }

    public static boolean isPlaybackEmit()
    {
        return captureSuspended > 0;
    }

    public static void suspendCapture()
    {
        captureSuspended++;
    }

    public static void resumeCapture()
    {
        captureSuspended = Math.max(0, captureSuspended - 1);
    }

    public static void capture(ParticleEffect parameters, double x, double y, double z)
    {
        if (captureSuspended > 0)
        {
            return;
        }
        ParticleRecorder recorder = current;

        if (recorder == null || !PovSettings.isBakeParticles() || parameters == null)
        {
            return;
        }

        ParticleType<?> type = parameters.getType();

        if (type == ParticleTypes.ENTITY_EFFECT
            || type == ParticleTypes.AMBIENT_ENTITY_EFFECT
            || type == ParticleTypes.EFFECT
            || type == ParticleTypes.TOTEM_OF_UNDYING
            || !isBurstEffect(type))
        {
            return;
        }

        recorder.pending.add(new Spawn(parameters, x, y, z));
    }

    private static String effectKey(ParticleEffect effect)
    {
        String id = Registries.PARTICLE_TYPE.getId(effect.getType()).toString();
        if (effect instanceof BlockStateParticleEffect block)
        {
            return id + ":" + Registries.BLOCK.getId(block.getBlockState().getBlock());
        }
        return id;
    }

    private static int visibleStatusColor(ClientPlayerEntity player)
    {
        int color = -1;

        for (StatusEffectInstance instance : player.getStatusEffects())
        {
            if (instance == null || !instance.shouldShowParticles())
            {
                continue;
            }

            color = instance.getEffectType().getColor();
        }

        return color;
    }

    private static boolean isBurstEffect(ParticleType<?> type)
    {
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
            || type == ParticleTypes.SPLASH
            || type == ParticleTypes.BUBBLE_POP
            || type == ParticleTypes.EXPLOSION
            || type == ParticleTypes.EXPLOSION_EMITTER
            || type == ParticleTypes.ANGRY_VILLAGER
            || type == ParticleTypes.HAPPY_VILLAGER
            || type == ParticleTypes.POOF
            || type == ParticleTypes.FIREWORK
            || type == ParticleTypes.FLASH;
    }

    private static void fillExtra(ParticleEffectPovActionClip clip, ParticleEffect parameters)
    {
        if (parameters instanceof BlockStateParticleEffect block)
        {
            Identifier blockId = Registries.BLOCK.getId(block.getBlockState().getBlock());
            clip.blockId.set(blockId.toString());
        }
        else if (parameters instanceof DustParticleEffect dust)
        {
            org.joml.Vector3f color = dust.getColor();
            clip.dustR.set(color.x());
            clip.dustG.set(color.y());
            clip.dustB.set(color.z());
            clip.dustScale.set(dust.getScale());
        }
    }

    private static final class Spawn
    {
        final ParticleEffect parameters;
        final double x;
        final double y;
        final double z;

        Spawn(ParticleEffect parameters, double x, double y, double z)
        {
            this.parameters = parameters;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class OpenEffect
    {
        final ParticleEffectPovActionClip clip;
        final String key;
        int lastTick;

        OpenEffect(ParticleEffectPovActionClip clip, String key, int tick)
        {
            this.clip = clip;
            this.key = key;
            this.lastTick = tick;
        }
    }
}
