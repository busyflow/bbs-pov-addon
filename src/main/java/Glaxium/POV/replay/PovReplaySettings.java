package Glaxium.POV.replay;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import java.lang.ref.WeakReference;

/** Shared replay-setting lookups used by recording and preview rendering. */
public final class PovReplaySettings
{
    private static WeakReference<UIFilmPanel> lastFilmPanel = new WeakReference<>(null);

    private PovReplaySettings()
    {
    }

    public static boolean isOverlayEnabled(Replay replay)
    {
        return replay instanceof ReplayPovAccess access
            && access.bbsPov$getOverlayEnabled().get();
    }

    public static boolean isCameraShakeEnabled(Replay replay)
    {
        return replay instanceof ReplayPovAccess access
            && access.bbsPov$getCameraShake().get();
    }

    public static Replay getSelectedReplay()
    {
        UIFilmPanel filmPanel = getFilmPanel();

        return filmPanel == null ? null : filmPanel.replayEditor.getReplay();
    }

    public static UIFilmPanel getFilmPanel()
    {
        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        if (currentMenu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIFilmPanel filmPanel)
            {
                lastFilmPanel = new WeakReference<>(filmPanel);
                return filmPanel;
            }
        }

        /* BBS hides the dashboard during full Film playback. The panel and its
         * Runner controller remain alive and continue advancing the cursor, but
         * UIScreen.getCurrentMenu() can no longer find them. Retain that panel
         * only for the duration of active playback; otherwise normal gameplay
         * must keep using Minecraft's own hand renderer. */
        UIFilmPanel filmPanel = lastFilmPanel.get();

        /* UIFilmController.isPlaying() cannot be called here: BBS clears its UI
         * context while the dashboard is hidden. RunnerCameraController is the
         * non-UI lifetime signal that survives full Film playback. */
        return filmPanel != null
            && BBSModClient.getCameraController().getCurrent() instanceof RunnerCameraController
            ? filmPanel
            : null;
    }

    public static boolean isPovMode()
    {
        UIFilmPanel filmPanel = getFilmPanel();

        return filmPanel != null
            && filmPanel.getController() != null
            && filmPanel.getController().getPovMode() == Glaxium.POV.camera.PovCameraMode.POV;
    }
}
