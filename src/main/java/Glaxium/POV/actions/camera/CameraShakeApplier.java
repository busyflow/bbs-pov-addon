package Glaxium.POV.actions.camera;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Applies baked Camera Shake clips with vanilla {@code tiltViewWhenHurt} math.
 */
public final class CameraShakeApplier
{
    private CameraShakeApplier()
    {
    }

    private static Sample cachedSample;
    private static float cachedTickDelta = Float.NaN;

    public static boolean shouldApply(float tickDelta)
    {
        return resolve(tickDelta) != null;
    }

    /** Always cancel leftover vanilla hurt tilt during Right-Ctrl playback. */
    public static boolean shouldCancelVanilla(float tickDelta)
    {
        cachedSample = resolve(tickDelta);
        cachedTickDelta = tickDelta;

        return PovPlaybackContext.getActive(tickDelta) != null || cachedSample != null;
    }

    public static void apply(MatrixStack matrices, float tickDelta)
    {
        Sample sample = tickDelta == cachedTickDelta ? cachedSample : resolve(tickDelta);
        cachedSample = null;
        cachedTickDelta = Float.NaN;

        if (sample == null)
        {
            return;
        }

        /* Live tickDelta keeps oscillating while the film cursor is frozen, which
         * makes hurtTime - tickDelta shutter in place. Hold delta at 0 when paused. */
        applySample(matrices, sample, frozenDelta(tickDelta));
    }

    private static float frozenDelta(float tickDelta)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);

        if (playback != null)
        {
            return playback.controller().paused ? 0F : tickDelta;
        }

        UIFilmPanel panel = PovReplaySettings.getFilmPanel();

        if (panel != null && (panel.getRunner() == null || !panel.getRunner().isRunning()))
        {
            return 0F;
        }

        return tickDelta;
    }

    private static Sample resolve(float tickDelta)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);

        if (playback != null)
        {
            return sampleFromReplay(playback.replay(), playback.replayTick());
        }

        /* Hands/HUD keep working when BBS hides the dashboard during film play
         * via PovReplaySettings — use that same panel lifetime for shake. */
        UIFilmPanel panel = PovReplaySettings.getFilmPanel();

        if (panel == null || panel.getData() == null)
        {
            return null;
        }

        int povMode = panel.getController().getPovMode();

        if (povMode == UIFilmController.CAMERA_MODE_FREE
            || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            return null;
        }

        Film film = (Film) panel.getData();
        int cursor = panel.getCursor();
        boolean playing = panel.getRunner() != null && panel.getRunner().isRunning();
        float transition = playing
            ? Math.max(0F, Math.min(1F, tickDelta))
            : 0F;
        float filmTick = cursor + transition;
        boolean povEditMode = povMode == PovCameraMode.POV;

        if (povEditMode)
        {
            Replay replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;

            if (replay == null)
            {
                replay = film.getFirstPersonReplay();
            }

            if (!PovReplaySettings.isCameraShakeEnabled(replay))
            {
                return null;
            }

            return sampleFromReplay(replay, replayTick(replay, cursor, transition));
        }

        PovCameraClip clip = PovCameraClips.resolve(film, filmTick);

        if (clip != null)
        {
            if (!clip.cameraShake.get())
            {
                return null;
            }

            Replay replay = PovCameraClips.resolveReplay(film, clip);

            return sampleFromReplay(replay, replayTick(replay, cursor, transition));
        }

        /* Camera / first-person film modes without a POV clip still match
         * Right-Ctrl: shake the configured first-person replay. */
        if (povMode == UIFilmController.CAMERA_MODE_CAMERA
            || povMode == UIFilmController.CAMERA_MODE_FIRST_PERSON)
        {
            Replay replay = film.getFirstPersonReplay();

            return sampleFromReplay(replay, replayTick(replay, cursor, transition));
        }

        return null;
    }

    private static float replayTick(Replay replay, int cursor, float transition)
    {
        if (replay == null)
        {
            return cursor + transition;
        }

        return replay.getTick(cursor) + transition;
    }

    private static Sample sampleFromReplay(Replay replay, float replayTick)
    {
        if (replay == null || !PovReplaySettings.isCameraShakeEnabled(replay) || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return null;
        }

        RecordedPovActions actions = access.bbsPov$getActions();

        if (actions == null)
        {
            return null;
        }

        CameraShakePovActionClip shake = actions.getActiveCameraShake(replayTick);

        if (shake == null)
        {
            return null;
        }

        float local = shake.getLocalTick(replayTick);
        boolean active = Boolean.TRUE.equals(shake.active.interpolate(local, false));

        if (!active)
        {
            return null;
        }

        int hurtTime = shake.hurtTime.interpolate(local, 0);
        int maxHurtTime = Math.max(1, shake.maxHurtTime.interpolate(local, 10));
        float yaw = shake.damageTiltYaw.interpolate(local, 0F);
        int deathTime = shake.deathTime.interpolate(local, 0);

        return new Sample(hurtTime, maxHurtTime, yaw, deathTime);
    }

    private static void applySample(MatrixStack matrices, Sample sample, float tickDelta)
    {
        if (sample.deathTime > 0)
        {
            float death = Math.min(sample.deathTime + tickDelta, 20F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(40F - 8000F / (death + 200F)));
        }

        float remaining = sample.hurtTime - tickDelta;

        if (remaining < 0F)
        {
            return;
        }

        float t = remaining / (float) sample.maxHurtTime;
        t = MathHelper.sin((t * t) * (t * t) * (float) Math.PI);

        float strength = 1F;
        var option = MinecraftClient.getInstance().options.getDamageTiltStrength();

        if (option != null)
        {
            strength = option.getValue().floatValue();
        }

        float roll = -t * 14F * strength;

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-sample.damageTiltYaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sample.damageTiltYaw));
    }

    private record Sample(int hurtTime, int maxHurtTime, float damageTiltYaw, int deathTime)
    {
    }
}
