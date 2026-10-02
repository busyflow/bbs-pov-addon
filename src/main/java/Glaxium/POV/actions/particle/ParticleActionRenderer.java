package Glaxium.POV.actions.particle;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.particle.recording.ParticleRecorder;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import Glaxium.POV.render.PovViewportMetrics;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Spawns real vanilla particles on the replay actor. Vanilla then ticks and
 * renders them, so they sit in 3D (orbit/free) instead of a fake overlay.
 */
public final class ParticleActionRenderer
{
    private static final Random RANDOM = new Random();
    private static int lastPlayingFilmTick = Integer.MIN_VALUE;

    private ParticleActionRenderer()
    {
    }

    public static void tick()
    {
        if (UIPovHandEditor.isActive() || ParticleRecorder.isArmed())
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world == null || client.particleManager == null)
        {
            return;
        }

        /* World playback (Right Ctrl / play film) even when first-person is off.
         * First-person-only lookup misses ordinary WorldFilmController actors. */
        BaseFilmController world = activeWorldController();
        UIFilmPanel panel = PovViewportMetrics.resolveFilmPanel();

        if (panel == null)
        {
            panel = PovReplaySettings.getFilmPanel();
        }

        Film film;
        int cursor;
        boolean playing;
        Map<String, IEntity> entities;

        if (world != null)
        {
            film = world.film;
            cursor = world.getTick();
            playing = !world.paused;
            entities = world.getEntities();
        }
        else if (panel != null && panel.getData() != null && panel.getController() != null)
        {
            film = (Film) panel.getData();
            cursor = panel.getCursor();
            playing = panel.getRunner() != null && panel.getRunner().isRunning();
            entities = panel.getController().getEntities();
        }
        else
        {
            lastPlayingFilmTick = Integer.MIN_VALUE;
            return;
        }

        if (film == null || entities == null)
        {
            return;
        }

        if (playing)
        {
            if (cursor == lastPlayingFilmTick)
            {
                return;
            }

            lastPlayingFilmTick = cursor;
        }
        else
        {
            lastPlayingFilmTick = Integer.MIN_VALUE;
        }

        List<Replay> replays = film.replays.getList();

        ParticleRecorder.suspendCapture();

        try
        {
            for (int replayIndex = 0; replayIndex < replays.size(); replayIndex++)
            {
                Replay replay = replays.get(replayIndex);

                if (!(replay.keyframes instanceof ReplayKeyframesPovAccess access))
                {
                    continue;
                }

                RecordedPovActions actions = access.bbsPov$getActions();
                if (actions == null)
                {
                    continue;
                }

                IEntity entity = entities.get(replay.getId());
                Vec3d origin;
                float width;
                float height;

                if (entity != null)
                {
                    origin = ParticleSpaces.lerpPos(entity, 1F);
                    width = ParticleSpaces.width(entity);
                    height = ParticleSpaces.height(entity);
                }
                else if (client.player != null)
                {
                    origin = client.player.getPos();
                    width = client.player.getWidth();
                    height = client.player.getHeight();
                }
                else if (client.cameraEntity != null)
                {
                    origin = client.cameraEntity.getPos();
                    width = client.cameraEntity.getWidth();
                    height = client.cameraEntity.getHeight();
                }
                else
                {
                    continue;
                }

                float replayTick = replay.getTick(cursor);
                boolean particleEmitted = false;

                RecordedHandData handData = access.bbsPov$getHand();
                if (handData != null)
                {
                    int wholeTick = (int) Math.floor(replayTick);
                    int active = handData.activeHand.interpolate(wholeTick, 0);
                    boolean showParticles = handData.showUseParticles.interpolate(replayTick, true);
                    if (active != 0 && showParticles)
                    {
                        emitEatingParticles(client, origin, width, height, entity, replay, replayTick, handData);
                        particleEmitted = true;
                    }
                }

                for (Clip clip : actions.get())
                {
                    if (clip instanceof ParticleEffectPovActionClip particleClip
                        && particleClip.isActive(replayTick))
                    {
                        if (!particleEmitted)
                        {
                            emit(client, origin, width, height, particleClip, entity, replay, replayTick);
                            particleEmitted = true;
                        }
                    }
                    else if (clip instanceof ScreenEffectPovActionClip screenClip
                        && screenClip.isActive(replayTick)
                        && screenClip.hasEffect("totem"))
                    {
                        emitTotem(client, origin, width, height, screenClip, replayTick - screenClip.tick.get());
                    }
                }
            }
        }
        finally
        {
            ParticleRecorder.resumeCapture();
        }
    }

    private static void emitTotem(
        MinecraftClient client,
        Vec3d origin,
        float width,
        float height,
        ScreenEffectPovActionClip clip,
        float localTick)
    {
        boolean particles = clip.totemParticles.isEmpty() ? false : clip.totemParticles.interpolate(localTick);
        if (!particles)
        {
            clip.lastTotemParticleTick = Integer.MIN_VALUE;
            return;
        }

        int curTick = (int) localTick;
        if (curTick < 0 || clip.lastTotemParticleTick == curTick)
        {
            return;
        }
        clip.lastTotemParticleTick = curTick;

        for (int i = 0; i < 16; ++i)
        {
            double d = (RANDOM.nextFloat() * 2.0F - 1.0F);
            double e = (RANDOM.nextFloat() * 2.0F - 1.0F);
            double f = (RANDOM.nextFloat() * 2.0F - 1.0F);

            if (d * d + e * e + f * f <= 1.0D)
            {
                double px = origin.x + (d / 4.0D) * (double) width;
                double py = origin.y + (0.5D + e / 4.0D) * (double) height;
                double pz = origin.z + (f / 4.0D) * (double) width;

                client.particleManager.addParticle(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz, d, e + 0.2D, f);
            }
        }
    }

    private static void emitEatingParticles(
        MinecraftClient client,
        Vec3d origin,
        float width,
        float height,
        IEntity entity,
        Replay replay,
        float replayTick,
        RecordedHandData handData)
    {
        int wholeTick = (int) Math.floor(replayTick);
        net.minecraft.item.ItemStack itemToUse = handData.activeItem.interpolate(wholeTick, net.minecraft.item.ItemStack.EMPTY);
        if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(net.minecraft.item.Items.AIR))
        {
            if (replay != null && replay.keyframes != null)
            {
                net.minecraft.item.ItemStack held = replay.keyframes.getMainHandStack(replayTick);
                if (held == null || held.isEmpty() || held.isOf(net.minecraft.item.Items.AIR))
                {
                    held = replay.keyframes.offHand.interpolate(replayTick, net.minecraft.item.ItemStack.EMPTY);
                }
                if (held != null && !held.isEmpty() && !held.isOf(net.minecraft.item.Items.AIR))
                {
                    itemToUse = held;
                }
            }
        }
        if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(net.minecraft.item.Items.AIR))
        {
            return;
        }

        net.minecraft.util.UseAction useAction = itemToUse.getUseAction();
        if (useAction != net.minecraft.util.UseAction.EAT && useAction != net.minecraft.util.UseAction.DRINK)
        {
            return;
        }

        ParticleEffect effect = new ItemStackParticleEffect(ParticleTypes.ITEM, itemToUse);

        float yaw = entity != null ? entity.getHeadYaw() : (replay != null && replay.keyframes != null ? (float) replay.keyframes.yaw.interpolate(replayTick, 0.0).doubleValue() : 0F);
        float pitch = entity != null ? entity.getPitch() : (replay != null && replay.keyframes != null ? (float) replay.keyframes.pitch.interpolate(replayTick, 0.0).doubleValue() : 0F);

        float radPitch = -pitch * 0.017453292F;
        float radYaw = -yaw * 0.017453292F;

        int count = 1 + RANDOM.nextInt(3);
        for (int i = 0; i < count; i++)
        {
            Vec3d vel = new Vec3d(((double) RANDOM.nextFloat() - 0.5D) * 0.1D, (double) RANDOM.nextFloat() * 0.1D + 0.1D, 0.0D)
                .rotateX(radPitch)
                .rotateY(radYaw);

            double d = (double) (-RANDOM.nextFloat()) * 0.4D - 0.2D;
            Vec3d offset = new Vec3d(((double) RANDOM.nextFloat() - 0.5D) * 0.3D, d, 0.6D)
                .rotateX(radPitch)
                .rotateY(radYaw);

            Vec3d pos = origin.add(0.0D, (double) height * 0.85D, 0.0D).add(offset);
            client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vel.x, vel.y + 0.05D, vel.z);
        }
    }

    private static void emit(
        MinecraftClient client,
        Vec3d origin,
        float width,
        float height,
        ParticleEffectPovActionClip clip,
        IEntity entity,
        Replay replay,
        float replayTick)
    {
        ParticleEffect effect = ParticleEffects.fromClip(clip);

        if (effect == null)
        {
            return;
        }

        if (effect.getType() == ParticleTypes.ITEM)
        {
            net.minecraft.item.ItemStack itemToUse = null;
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access)
            {
                RecordedHandData handData = access.bbsPov$getHand();
                if (handData != null)
                {
                    int wholeTick = (int) Math.floor(replayTick);
                    net.minecraft.item.ItemStack active = handData.activeItem.interpolate(wholeTick, net.minecraft.item.ItemStack.EMPTY);
                    if (active != null && !active.isEmpty() && !active.isOf(net.minecraft.item.Items.AIR))
                    {
                        itemToUse = active;
                    }
                }
            }
            if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(net.minecraft.item.Items.AIR))
            {
                itemToUse = clip.extraItem();
            }
            if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(net.minecraft.item.Items.AIR))
            {
                if (replay != null && replay.keyframes != null)
                {
                    net.minecraft.item.ItemStack held = replay.keyframes.getMainHandStack(replayTick);
                    if (held == null || held.isEmpty() || held.isOf(net.minecraft.item.Items.AIR))
                    {
                        held = replay.keyframes.offHand.interpolate(replayTick, net.minecraft.item.ItemStack.EMPTY);
                    }
                    if (held != null && !held.isEmpty() && !held.isOf(net.minecraft.item.Items.AIR))
                    {
                        itemToUse = held;
                    }
                }
            }
            if (itemToUse == null || itemToUse.isEmpty() || itemToUse.isOf(net.minecraft.item.Items.AIR))
            {
                itemToUse = new net.minecraft.item.ItemStack(net.minecraft.item.Items.APPLE);
            }
            effect = new ItemStackParticleEffect(ParticleTypes.ITEM, itemToUse);

            float yaw = entity != null ? entity.getHeadYaw() : (replay != null ? (float) replay.keyframes.yaw.interpolate(replayTick, 0.0).doubleValue() : 0F);
            float pitch = entity != null ? entity.getPitch() : (replay != null ? (float) replay.keyframes.pitch.interpolate(replayTick, 0.0).doubleValue() : 0F);

            float radPitch = -pitch * 0.017453292F;
            float radYaw = -yaw * 0.017453292F;

            int count = 1 + RANDOM.nextInt(3);
            for (int i = 0; i < count; i++)
            {
                Vec3d vel = new Vec3d(((double) RANDOM.nextFloat() - 0.5D) * 0.1D, (double) RANDOM.nextFloat() * 0.1D + 0.1D, 0.0D)
                    .rotateX(radPitch)
                    .rotateY(radYaw);

                double d = (double) (-RANDOM.nextFloat()) * 0.4D - 0.2D;
                Vec3d offset = new Vec3d(((double) RANDOM.nextFloat() - 0.5D) * 0.3D, d, 0.6D)
                    .rotateX(radPitch)
                    .rotateY(radYaw);

                Vec3d pos = origin.add(0.0D, (double) height * 0.85D, 0.0D).add(offset);
                client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vel.x, vel.y + 0.05D, vel.z);
            }
            return;
        }

        boolean status = effect.getType() == ParticleTypes.ENTITY_EFFECT
            || effect.getType() == ParticleTypes.AMBIENT_ENTITY_EFFECT;

        if (status && !RANDOM.nextBoolean())
        {
            return;
        }

        if (effect.getType() == ParticleTypes.BLOCK)
        {
            Vec3d pos = origin.add((RANDOM.nextDouble() - 0.5D) * width, 0.1D, (RANDOM.nextDouble() - 0.5D) * width);
            float yaw = entity != null ? entity.getHeadYaw() : (replay != null && replay.keyframes != null ? (float) replay.keyframes.yaw.interpolate(replayTick, 0.0).doubleValue() : 0F);
            float radYaw = (float) Math.toRadians(yaw);
            double vx = Math.sin(radYaw) * 0.15D + (RANDOM.nextDouble() - 0.5D) * 0.1D;
            double vy = 0.15D + RANDOM.nextDouble() * 0.1D;
            double vz = -Math.cos(radYaw) * 0.15D + (RANDOM.nextDouble() - 0.5D) * 0.1D;
            client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vx, vy, vz);
            return;
        }

        Vec3d pos = ParticleSpaces.pointInActorAabb(origin, width, height, RANDOM);
        double vx;
        double vy;
        double vz;

        if (status)
        {
            vx = clip.dustR.get();
            vy = clip.dustG.get();
            vz = clip.dustB.get();
        }
        else
        {
            vx = (RANDOM.nextDouble() - 0.5D) * 0.15D;
            vy = RANDOM.nextDouble() * 0.2D;
            vz = (RANDOM.nextDouble() - 0.5D) * 0.15D;
        }

        client.particleManager.addParticle(effect, pos.x, pos.y, pos.z, vx, vy, vz);
    }

    /** BBS camera / world playback: hide the live player's vanilla particles. */
    public static boolean hidesLivePlayerParticles()
    {
        if (ParticleRecorder.isArmed() || ParticleRecorder.isPlaybackEmit())
        {
            return false;
        }

        if (activeWorldController() != null)
        {
            return true;
        }

        UIFilmPanel panel = PovViewportMetrics.resolveFilmPanel();

        if (panel == null)
        {
            panel = PovReplaySettings.getFilmPanel();
        }

        return panel != null && panel.getData() != null;
    }

    private static BaseFilmController activeWorldController()
    {
        List<BaseFilmController> controllers =
            ((FilmsPovAccess) BBSModClient.getFilms()).bbsPov$getControllers();

        if (controllers == null || controllers.isEmpty())
        {
            return null;
        }

        for (int i = controllers.size() - 1; i >= 0; i--)
        {
            BaseFilmController controller = controllers.get(i);

            if (controller != null && !controller.hasFinished())
            {
                return controller;
            }
        }

        return null;
    }
}
