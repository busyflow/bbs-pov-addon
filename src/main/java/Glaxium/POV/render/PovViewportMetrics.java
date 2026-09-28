package Glaxium.POV.render;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.glfw.GLFW;

public final class PovViewportMetrics {
   private PovViewportMetrics() {
   }

   public static int getRealWindowWidth() {
      MinecraftClient client = MinecraftClient.getInstance();
      long handle = client.getWindow().getHandle();
      int[] w = new int[1];
      int[] h = new int[1];
      GLFW.glfwGetFramebufferSize(handle, w, h);
      return w[0] > 0 ? w[0] : client.getWindow().getWidth();
   }

   public static int getRealWindowHeight() {
      MinecraftClient client = MinecraftClient.getInstance();
      long handle = client.getWindow().getHandle();
      int[] w = new int[1];
      int[] h = new int[1];
      GLFW.glfwGetFramebufferSize(handle, w, h);
      return h[0] > 0 ? h[0] : client.getWindow().getHeight();
   }

   public static int getMinecraftGuiScale() {
      MinecraftClient client = MinecraftClient.getInstance();
      int guiScale = (Integer)client.options.getGuiScale().getValue();
      boolean forceUnicode = client.forcesUnicodeFont();
      if (guiScale > 0) {
         int scale = guiScale;
         if (forceUnicode && guiScale % 2 != 0) {
            scale = guiScale + 1;
         }

         return Math.max(1, scale);
      } else {
         int winWidth = getRealWindowWidth();
         int winHeight = getRealWindowHeight();
         int scale = 1;

         while (scale < winWidth && scale < winHeight && winWidth / (scale + 1) >= 320 && winHeight / (scale + 1) >= 240) {
            scale++;
         }

         if (forceUnicode && scale % 2 != 0) {
            scale++;
         }

         return Math.max(1, scale);
      }
   }

   public static int getFilmScaledWidth() {
      int videoW = BBSRendering.getVideoWidth();
      int videoH = BBSRendering.getVideoHeight();
      if (videoW > 0 && videoH > 0) {
         double videoAspect = (double)videoW / (double)videoH;
         return Math.max(1, (int)Math.round((double)getFilmScaledHeight() * videoAspect));
      } else {
         return getMinecraftScaledWidth();
      }
   }

   public static int getFilmScaledHeight() {
      MinecraftClient client = MinecraftClient.getInstance();
      int winHeight = getRealWindowHeight();
      if (winHeight <= 0) {
         winHeight = client.getWindow().getFramebufferHeight();
      }

      if (winHeight <= 0) {
         return client.getWindow().getScaledHeight();
      } else {
         int scale = getMinecraftGuiScale();
         return Math.max(1, (int)Math.ceil((double)winHeight / (double)scale));
      }
   }

   public static int getMinecraftScaledWidth() {
      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer fb = client.getFramebuffer();
      int fbWidth = fb != null && fb.textureWidth > 0 ? fb.textureWidth : client.getWindow().getFramebufferWidth();
      int fbHeight = fb != null && fb.textureHeight > 0 ? fb.textureHeight : client.getWindow().getFramebufferHeight();
      int winHeight = getRealWindowHeight();
      if (winHeight <= 0) {
         winHeight = fbHeight;
      }

      int scale = getMinecraftGuiScale();
      double effectiveScale = (double)scale * ((double)fbHeight / (double)winHeight);
      if (effectiveScale <= 1.0E-4) {
         effectiveScale = 1.0;
      }

      return Math.max(1, (int)Math.round((double)fbWidth / effectiveScale));
   }

   public static int getMinecraftScaledHeight() {
      MinecraftClient client = MinecraftClient.getInstance();
      Framebuffer fb = client.getFramebuffer();
      int fbHeight = fb != null && fb.textureHeight > 0 ? fb.textureHeight : client.getWindow().getFramebufferHeight();
      int winHeight = getRealWindowHeight();
      if (winHeight <= 0) {
         winHeight = fbHeight;
      }

      int scale = getMinecraftGuiScale();
      double effectiveScale = (double)scale * ((double)fbHeight / (double)winHeight);
      if (effectiveScale <= 1.0E-4) {
         effectiveScale = 1.0;
      }

      return Math.max(1, (int)Math.round((double)fbHeight / effectiveScale));
   }

   public static UIFilmPanel resolveFilmPanel() {
      if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard) {
         UIDashboardPanel panel = dashboard.getPanels().panel;
         if (panel instanceof UIFilmPanel) {
            return (UIFilmPanel)panel;
         }
      }

      return null;
   }
}
