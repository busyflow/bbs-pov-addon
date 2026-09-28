package Glaxium.POV.replay;

import java.lang.ref.WeakReference;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

public final class PovReplaySettings {
   private static WeakReference<UIFilmPanel> lastFilmPanel = new WeakReference<>(null);

   private PovReplaySettings() {
   }

   public static boolean isOverlayEnabled(Replay replay) {
      if (replay instanceof ReplayPovAccess access && (Boolean)access.bbsPov$getOverlayEnabled().get()) {
         return true;
      }

      return false;
   }

   public static boolean isCameraShakeEnabled(Replay replay) {
      if (replay instanceof ReplayPovAccess access && (Boolean)access.bbsPov$getCameraShake().get()) {
         return true;
      }

      return false;
   }

   public static Replay getSelectedReplay() {
      UIFilmPanel filmPanel = getFilmPanel();
      return filmPanel == null ? null : filmPanel.replayEditor.getReplay();
   }

   public static UIFilmPanel getFilmPanel() {
      if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard && dashboard.getPanels().panel instanceof UIFilmPanel filmPanel) {
         lastFilmPanel = new WeakReference<>(filmPanel);
         return filmPanel;
      }

      UIFilmPanel filmPanel = lastFilmPanel.get();
      return filmPanel != null && BBSModClient.getCameraController().getCurrent() instanceof RunnerCameraController ? filmPanel : null;
   }

   public static boolean isPovMode() {
      UIFilmPanel filmPanel = getFilmPanel();
      return filmPanel != null && filmPanel.getController() != null && filmPanel.getController().getPovMode() == 6;
   }
}
