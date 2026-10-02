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
 * Resolves the keyframe-driven Blindness intensity factor (0.0 to 1.0) and blindness fog radius (in blocks)
 * for active ScreenEffectPovActionClips during BBS film playback and film editor preview.
 */
public final class PovBlindnessHelper
{
    public static final float DEFAULT_RADIUS = 5.0F;

    private PovBlindnessHelper()
    {
    }

    public static float resolveBlindnessFactor(float tickDelta)
    {
        Sample sample = resolve(tickDelta);
        return sample != null ? sample.factor : -1.0F;
    }

    public static float resolveBlindnessRadius(float tickDelta)
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
            if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("blindness"))
            {
                float elapsed = se.getLocalTick(replayTick);
                float opacity = se.blindnessOpacity.isEmpty() ? 1.0F : se.blindnessOpacity.interpolate(elapsed);
                float radius = se.blindnessRadius.isEmpty() ? DEFAULT_RADIUS : se.blindnessRadius.interpolate(elapsed);
                return new Sample(MathHelper.clamp(opacity, 0.0F, 1.0F), Math.max(0.5F, radius));
            }
        }

        return null;
    }

    private record Sample(float factor, float radius) {}
}
