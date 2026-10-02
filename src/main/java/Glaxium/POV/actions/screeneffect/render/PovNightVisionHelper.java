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
 * Resolves the keyframe-driven Night Vision brightness factor (0.0 to 1.0)
 * for active ScreenEffectPovActionClips during BBS film playback and film editor preview.
 */
public final class PovNightVisionHelper
{
    private PovNightVisionHelper()
    {
    }

    public static float resolveNightVisionStrength(float tickDelta)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
        if (playback != null)
        {
            return sampleFromReplay(playback.replay(), playback.replayTick());
        }

        UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        if (panel == null || panel.getData() == null)
        {
            return -1.0F;
        }

        int povMode = panel.getController().getPovMode();
        if (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            return -1.0F;
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
                return -1.0F;
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

        return -1.0F;
    }

    private static float sampleFromReplay(Replay replay, float replayTick)
    {
        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return -1.0F;
        }

        RecordedPovActions actions = access.bbsPov$getActions();
        if (actions == null)
        {
            return -1.0F;
        }

        for (Clip clip : actions.get())
        {
            if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick) && se.hasEffect("night_vision"))
            {
                float elapsed = se.getLocalTick(replayTick);
                boolean isVisible = se.nightVisionVisible.isEmpty() ? true : se.nightVisionVisible.interpolate(elapsed);
                if (!isVisible)
                {
                    return 0.0F;
                }
                float opacity = se.nightVisionOpacity.isEmpty() ? 1.0F : se.nightVisionOpacity.interpolate(elapsed);
                return MathHelper.clamp(opacity, 0.0F, 1.0F);
            }
        }

        return -1.0F;
    }
}
