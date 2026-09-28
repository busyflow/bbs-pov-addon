package Glaxium.POV.utils;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class PovEffectSuppression {
   private PovEffectSuppression() {
   }

   public static boolean isBbsActive() {
      if (PovPlaybackContext.getActive() == null && PovReplaySettings.getFilmPanel() == null) {
         Screen screen = MinecraftClient.getInstance().currentScreen;
         return screen instanceof UIScreen || UIScreen.getCurrentMenu() != null;
      } else {
         return true;
      }
   }
}
