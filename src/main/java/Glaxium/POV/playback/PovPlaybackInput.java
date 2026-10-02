package Glaxium.POV.playback;

import Glaxium.POV.integration.access.bbs.BBSModClientAccess;
import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Input lock for world playback, not the film editor's preview or recording. */
public final class PovPlaybackInput
{
    private PovPlaybackInput() {}

    /** True when any film playback (first or third person) is active in the world. */
    public static boolean isWorldPlaybackRunning()
    {
        Films films = BBSModClient.getFilms();
        if (films == null)
        {
            return false;
        }

        List<BaseFilmController> controllers = ((FilmsPovAccess) films).bbsPov$getControllers();
        if (controllers != null)
        {
            for (BaseFilmController controller : controllers)
            {
                if (!controller.hasFinished())
                {
                    return true;
                }
            }
        }

        return false;
    }

    /** True only when active first-person POV playback is running. */
    public static boolean isFirstPersonPlayback()
    {
        Films films = BBSModClient.getFilms();
        if (films == null || films.getRecorder() != null)
        {
            return false;
        }

        return PovPlaybackContext.getActive() != null;
    }

    /** Returns true if full input lock should apply (first-person playback). */
    public static boolean isLocked()
    {
        return isFirstPersonPlayback();
    }

    public static boolean isAltKey(int key)
    {
        return key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    public static boolean isAllowed(int key, int scancode)
    {
        try
        {
            KeyBinding playFilm = BBSModClientAccess.bbsPov$getKeyPlayFilm();
            if (playFilm != null && playFilm.matchesKey(key, scancode))
            {
                return true;
            }

            KeyBinding recordVideo = BBSModClientAccess.bbsPov$getKeyRecordVideo();
            if (recordVideo != null && recordVideo.matchesKey(key, scancode))
            {
                return true;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null && mc.options.togglePerspectiveKey != null)
            {
                if (mc.options.togglePerspectiveKey.matchesKey(key, scancode))
                {
                    return true;
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        return key == GLFW.GLFW_KEY_RIGHT_CONTROL || key == GLFW.GLFW_KEY_F4 || key == GLFW.GLFW_KEY_F5;
    }
}
