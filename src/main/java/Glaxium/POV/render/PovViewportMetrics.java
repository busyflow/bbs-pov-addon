package Glaxium.POV.render;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import net.minecraft.client.MinecraftClient;

/** Film vs Minecraft GUI scale and the active Film panel. */
public final class PovViewportMetrics
{
    private PovViewportMetrics()
    {
    }

    public static int getRealWindowWidth()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        int[] w = new int[1];
        int[] h = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(handle, w, h);
        return w[0] > 0 ? w[0] : client.getWindow().getWidth();
    }

    public static int getRealWindowHeight()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        int[] w = new int[1];
        int[] h = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(handle, w, h);
        return h[0] > 0 ? h[0] : client.getWindow().getHeight();
    }

    public static int getMinecraftGuiScale()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        int guiScale = client.options.getGuiScale().getValue();
        boolean forceUnicode = client.forcesUnicodeFont();

        if (guiScale > 0)
        {
            int scale = guiScale;
            if (forceUnicode && scale % 2 != 0)
            {
                scale++;
            }
            return Math.max(1, scale);
        }

        int winWidth = getRealWindowWidth();
        int winHeight = getRealWindowHeight();

        int scale = 1;
        while (scale < winWidth
            && scale < winHeight
            && winWidth / (scale + 1) >= 320
            && winHeight / (scale + 1) >= 240)
        {
            scale++;
        }

        if (forceUnicode && scale % 2 != 0)
        {
            scale++;
        }

        return Math.max(1, scale);
    }

    public static int getFilmGuiScale()
    {
        if (!BBSRendering.isCustomSize())
        {
            return getMinecraftGuiScale();
        }

        int videoW = BBSRendering.getVideoWidth();
        int videoH = BBSRendering.getVideoHeight();
        if (videoW <= 0 || videoH <= 0)
        {
            return getMinecraftGuiScale();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int guiScale = client.options.getGuiScale().getValue();
        boolean forceUnicode = client.forcesUnicodeFont();

        if (guiScale > 0)
        {
            int winHeight = getRealWindowHeight();
            if (winHeight > 0 && videoH != winHeight)
            {
                double factor = (double) videoH / (double) winHeight;
                int scaled = (int) Math.round(guiScale * factor);
                if (forceUnicode && scaled % 2 != 0)
                {
                    scaled++;
                }
                return Math.max(1, scaled);
            }

            int scale = guiScale;
            if (forceUnicode && scale % 2 != 0)
            {
                scale++;
            }
            return Math.max(1, scale);
        }

        int scale = 1;
        while (scale < videoW
            && scale < videoH
            && videoW / (scale + 1) >= 320
            && videoH / (scale + 1) >= 240)
        {
            scale++;
        }

        if (forceUnicode && scale % 2 != 0)
        {
            scale++;
        }

        return Math.max(1, scale);
    }

    public static int getFilmScaledWidth()
    {
        if (!BBSRendering.isCustomSize())
        {
            return getMinecraftScaledWidth();
        }

        int videoW = BBSRendering.getVideoWidth();
        int videoH = BBSRendering.getVideoHeight();
        if (videoW <= 0 || videoH <= 0)
        {
            return getMinecraftScaledWidth();
        }
        int scale = getFilmGuiScale();
        return Math.max(1, (int) Math.ceil((double) videoW / (double) scale));
    }

    public static int getFilmScaledHeight()
    {
        if (!BBSRendering.isCustomSize())
        {
            return getMinecraftScaledHeight();
        }

        int videoW = BBSRendering.getVideoWidth();
        int videoH = BBSRendering.getVideoHeight();
        if (videoW <= 0 || videoH <= 0)
        {
            return getMinecraftScaledHeight();
        }
        int scale = getFilmGuiScale();
        return Math.max(1, (int) Math.ceil((double) videoH / (double) scale));
    }

    public static int getMinecraftScaledWidth()
    {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public static int getMinecraftScaledHeight()
    {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    public static UIFilmPanel resolveFilmPanel()
    {
        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        if (currentMenu instanceof UIDashboard dashboard)
        {
            UIDashboardPanel panel = dashboard.getPanels().panel;

            if (panel instanceof UIFilmPanel filmPanel)
            {
                return filmPanel;
            }
        }

        return null;
    }
}
