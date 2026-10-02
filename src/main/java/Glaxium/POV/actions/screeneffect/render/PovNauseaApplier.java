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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/**
 * Applies the authentic vanilla 3D projection matrix wobble / world distortion
 * for active Nausea ScreenEffect clips during film playback and film editor preview.
 */
public final class PovNauseaApplier
{
    private PovNauseaApplier()
    {
    }

    public static void apply(MatrixStack matrixStack, float tickDelta, int ticks)
    {
        Sample sample = resolve(tickDelta, ticks);
        if (sample == null || sample.intensity <= 0.001F)
        {
            return;
        }

        float nauseaIntensity = sample.intensity;
        float scale = 5.0F / (nauseaIntensity * nauseaIntensity + 5.0F) - nauseaIntensity * 0.04F;
        scale *= scale;

        float angle = sample.time * (float) sample.speed;

        RotationAxis axis = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
        matrixStack.multiply(axis.rotationDegrees(angle));
        matrixStack.scale(1.0F / scale, 1.0F, 1.0F);
        matrixStack.multiply(axis.rotationDegrees(-angle));
    }

    private static Sample resolve(float tickDelta, int ticks)
    {
        PovPlaybackContext.Frame playback = PovPlaybackContext.getActive(tickDelta);
        if (playback != null)
        {
            return sampleFromReplay(playback.replay(), playback.replayTick(), playback.replayTick());
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
            return sampleFromReplay(replay, rTick, filmTick);
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
            return sampleFromReplay(replay, rTick, filmTick);
        }

        if (povMode == UIFilmController.CAMERA_MODE_CAMERA || povMode == UIFilmController.CAMERA_MODE_FIRST_PERSON)
        {
            Replay replay = film.getFirstPersonReplay();
            float rTick = replay != null ? replay.getTick(cursor) + transition : filmTick;
            return sampleFromReplay(replay, rTick, filmTick);
        }

        return null;
    }

    private static Sample sampleFromReplay(Replay replay, float replayTick, float time)
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
            if (clip instanceof ScreenEffectPovActionClip se && se.isActive(replayTick))
            {
                float elapsed = se.getLocalTick(replayTick);
                boolean hasNausea = se.hasEffect("nausea");
                boolean hasPortal = se.hasEffect("portal");

                if (hasNausea || hasPortal)
                {
                    float intensity = 0.0F;
                    if (hasNausea)
                    {
                        float distortion = se.nauseaDistortion.isEmpty() ? 1.0F : se.nauseaDistortion.interpolate(elapsed);
                        float opacity = se.nauseaOpacity.isEmpty() ? 1.0F : se.nauseaOpacity.interpolate(elapsed);
                        intensity = Math.max(intensity, distortion * opacity);
                    }
                    if (hasPortal)
                    {
                        float portalOp = se.portalOpacity.isEmpty() ? 1.0F : se.portalOpacity.interpolate(elapsed);
                        intensity = Math.max(intensity, portalOp);
                    }

                    if (intensity > 0.001F)
                    {
                        int speed = hasPortal ? 20 : 7;
                        return new Sample(intensity, time, speed);
                    }
                }
            }
        }

        return null;
    }

    private record Sample(float intensity, float time, int speed) {}
}
