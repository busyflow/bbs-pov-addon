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
 * Resolves keyframe-driven camera FOV zoom multiplier for active
 * ScreenEffectPovActionClips (spyglass effect) during BBS film playback and film editor preview.
 */
public final class PovSpyglassZoomHelper
{
    private PovSpyglassZoomHelper()
    {
    }

    public static float resolveZoomMultiplier(float tickDelta)
    {
        if (PovPlaybackContext.getActive(tickDelta) == null && !Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            return 1.0F;
        }

        // 1. Playback context (in-world film playback or camera runner)
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
        if (playback != null)
        {
            return sampleFromReplay(playback.replay(), playback.replayTick());
        }

        // 2. Film Editor timeline preview (ONLY when Film Panel is actively open on screen)
        UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        if (panel == null || !panel.hasParent() || !panel.isVisible() || panel.getData() == null)
        {
            return 1.0F;
        }

        int povMode = panel.getController().getPovMode();
        if (povMode == UIFilmController.CAMERA_MODE_FREE || povMode == UIFilmController.CAMERA_MODE_ORBIT)
        {
            return 1.0F;
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
            if (!clip.isFirstPerson() || !clip.actions.get() || !clip.screenEffects.get())
            {
                return 1.0F;
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

        return 1.0F;
    }

    public static float sampleFromReplay(Replay replay, float replayTick)
    {
        if (replay == null || !(replay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return 1.0F;
        }

        RecordedPovActions actions = access.bbsPov$getActions();
        if (actions == null)
        {
            return 1.0F;
        }

        float totalMultiplier = 1.0F;
        for (Clip clip : actions.get())
        {
            if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick))
            {
                float elapsed = se.getLocalTick(replayTick);
                boolean spyglassActive = false;
                if (se.hasEffect("spyglass") && !se.spyglassZoom.isEmpty())
                {
                    float zoom = se.spyglassZoom.interpolate(elapsed);
                    if (zoom > 0.0001F && Math.abs(zoom - 1.0F) > 0.001F)
                    {
                        totalMultiplier *= convertZoomToMultiplier(zoom);
                        spyglassActive = true;
                    }
                }

                if (!spyglassActive && se.hasEffect("frost") && !se.frostZoom.isEmpty())
                {
                    float zoom = se.frostZoom.interpolate(elapsed);
                    if (zoom > 0.001F)
                    {
                        totalMultiplier *= MathHelper.clamp(zoom, 0.1F, 2.0F);
                    }
                }
            }
        }

        return totalMultiplier;
    }

    public static float convertZoomToMultiplier(float zoom)
    {
        if (zoom <= 0.0001F)
        {
            return 1.0F;
        }
        if (zoom <= 1.0F)
        {
            return MathHelper.clamp(zoom, 0.005F, 1.0F);
        }
        return MathHelper.clamp(1.0F / zoom, 0.005F, 1.0F);
    }
}
