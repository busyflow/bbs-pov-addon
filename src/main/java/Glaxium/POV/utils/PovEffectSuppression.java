package Glaxium.POV.utils;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;

/**
 * Utility to determine if BBS film editing, film playback, or POV replay playback is currently active.
 * Only suppresses live player screen effects and status effects during film operations, leaving standard
 * Minecraft gameplay and other BBS menus (e.g. Model block menu, Form editor, Settings) completely unaffected.
 */
public final class PovEffectSuppression
{
    private PovEffectSuppression() {}

    public static boolean isBbsActive()
    {
        if (PovPlaybackContext.getActive() != null || PovReplaySettings.getFilmPanel() != null)
        {
            return true;
        }

        return BBSModClient.getCameraController().getCurrent() instanceof RunnerCameraController;
    }
}
