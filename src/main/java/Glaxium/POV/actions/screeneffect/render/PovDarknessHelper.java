package Glaxium.POV.actions.screeneffect.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.util.math.MathHelper;

/**
 * Resolves the keyframe-driven Darkness pulsation factor (0.0 to 1.0) and darkness fog radius (in blocks)
 * for active ScreenEffectPovActionClips during BBS film playback and film editor preview.
 */
public final class PovDarknessHelper
{
    public static final float DEFAULT_RADIUS = 15.0F;

    private PovDarknessHelper()
    {
    }

    public static float resolveDarknessFactor(float tickDelta)
    {
        Sample sample = resolve(tickDelta);
        return sample != null ? sample.fogFactor : -1.0F;
    }

    public static float resolveDarknessLightFactor(float tickDelta)
    {
        Sample sample = resolve(tickDelta);
        return sample != null ? sample.lightFactor : -1.0F;
    }

    public static float resolveDarknessRadius(float tickDelta)
    {
        Sample sample = resolve(tickDelta);
        return sample != null ? sample.radius : DEFAULT_RADIUS;
    }

    private static Sample resolve(float tickDelta)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
        if (playback != null)
        {
            return sampleFromReplay(playback.replay(), playback.replayTick());
        }

        UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        if (panel == null || panel.getData() == null)
        {
            return null;
        }

        int povMode = panel.getController().getPovMode();
        if (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            return null;
        }

        Film film = (Film) panel.getData();
        int cursor = panel.getCursor();
        boolean playing = panel.getRunner() != null && panel.getRunner().isRunning();
        float transition = playing ? Math.max(0F, Math.min(1F, tickDelta)) : 0F;
        float filmTick = cursor + transition;
        boolean povEditMode = povMode == PovCameraMode.POV;

        if (povEditMode)
        {
            Replay replay = panel.replayEditor != null ? panel.replayEditor.getReplay() : null;
            if (replay == null)
            {
                replay = film.getFirstPersonReplay();
            }
            float rTick = replay != null ? replay.getTick(cursor) + transition : filmTick;
            return sampleFromReplay(replay, rTick);
        }

        PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
        if (clip != null)
        {
            if (!clip.actions.get())
            {
                return null;
            }
            Replay replay = PovCameraClips.resolveReplay(film, clip);
            float rTick = replay != null ? replay.getTick(cursor) + transition : filmTick;
            return sampleFromReplay(replay, rTick);
        }

        if (povMode == UIFilmController.CAMERA_MODE_CAMERA || povMode == UIFilmController.CAMERA_MODE_FIRST_PERSON)
        {
            Replay replay = film.getFirstPersonReplay();
            float rTick = replay != null ? replay.getTick(cursor) + transition : filmTick;
            return sampleFromReplay(replay, rTick);
        }

        return null;
    }

    private static Sample sampleFromReplay(Replay replay, float replayTick)
    {
        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return null;
        }

        RecordedPovActions actions = access.bbsPov$getActions();
        if (actions == null)
        {
            return null;
        }

        for (Clip clip : actions.get())
        {
            if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("darkness"))
            {
                float elapsed = se.getLocalTick(replayTick);
                float opacity = se.darknessOpacity.isEmpty() ? 1.0F : se.darknessOpacity.interpolate(elapsed);
                float radius = se.darknessRadius.isEmpty() ? DEFAULT_RADIUS : se.darknessRadius.interpolate(elapsed);

                float clampedOpacity = MathHelper.clamp(opacity, 0.0F, 1.0F);
                // Vanilla heartbeat pulse (period: 120 ticks = 6 seconds, gentle dimming between 0.0 and 0.40)
                float pulse = 0.20F + 0.20F * (float) Math.sin((double) elapsed * (Math.PI * 2.0 / 120.0));
                float lightFactor = MathHelper.clamp(clampedOpacity * pulse, 0.0F, 0.40F);
                float fogFactor = clampedOpacity;

                return new Sample(fogFactor, lightFactor, Math.max(1.0F, radius));
            }
        }

        return null;
    }

    private record Sample(float fogFactor, float lightFactor, float radius) {}
}
