package Glaxium.POV.playback;

import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.film.replays.Replay;

import java.util.List;

/** The actual BBS Right-Control playback source, independent from every Film editor cursor. */
public final class PovPlaybackContext
{
    private static FirstPersonFilmController exportController;
    private static int exportBaseFrame;
    private static float exportBaseCursor;
    private static boolean exportClockActive;

    public record Frame(
        FirstPersonFilmController controller,
        Film film,
        Replay replay,
        PovCameraClip clip,
        int filmTick,
        float replayTick)
    {}

    private PovPlaybackContext() {}

    public static Frame getActive()
    {
        return getActive(0F);
    }

    /** Match BBS's own render-time sampling: the controller owns the whole
     * tick and render tickDelta supplies the fractional transition. */
    public static Frame getActive(float tickDelta)
    {
        List<BaseFilmController> controllers =
            ((FilmsPovAccess) BBSModClient.getFilms()).bbsPov$getControllers();

        /* The latest controller is the film most recently started by Right Ctrl. */
        for (int i = controllers.size() - 1; i >= 0; i--)
        {
            BaseFilmController candidate = controllers.get(i);

            if (!(candidate instanceof FirstPersonFilmController firstPerson))
            {
                continue;
            }

            Film film = firstPerson.film;
            // Replay playback is a film-wide choice, not the editor selection
            // or a POV clip left behind by legacy migration.
            Replay replay = film.getFirstPersonReplay();
            if (replay == null || firstPerson.hasFinished())
            {
                continue;
            }
            int filmTick = firstPerson.getTick();
            float transition = firstPerson.paused
                ? 0F
                : Math.max(0F, Math.min(1F, tickDelta));
            float filmCursor = getFilmCursor(firstPerson, filmTick + transition);
            int wholeCursor = (int) Math.floor(filmCursor);
            float cursorTransition = filmCursor - wholeCursor;
            PovCameraClip clip = PovCameraClips.resolve(film, filmCursor);
            if (clip == null)
            {
                clip = new PovCameraClip();
                clip.selector.set(PovCameraClips.indexOfReplay(film, replay));
            }
            else
            {
                Replay clipReplay = PovCameraClips.resolveReplay(film, clip);
                if (clipReplay != null)
                {
                    replay = clipReplay;
                }
            }

            if (clip != null && replay != null)
            {
                return new Frame(
                    firstPerson,
                    film,
                    replay,
                    clip,
                    wholeCursor,
                    replay.getTick(wholeCursor) + cursorTransition);
            }
        }

        exportClockActive = false;
        exportController = null;

        return null;
    }

    /** During F4, BBS renders at a synthetic fixed rate. Its exposed tickDelta
     * can wrap before every consumer observes the matching controller tick.
     * Anchor to the recorder frame counter so POV sampling is monotonic and
     * exactly one output-frame step apart. */
    private static float getFilmCursor(
        FirstPersonFilmController controller,
        float normalCursor)
    {
        var recorder = BBSModClient.getVideoRecorder();

        if (!recorder.isRecording())
        {
            exportClockActive = false;
            exportController = null;
            return normalCursor;
        }

        int frame = recorder.getCounter();

        if (!exportClockActive || exportController != controller || frame < exportBaseFrame)
        {
            exportClockActive = true;
            exportController = controller;
            exportBaseFrame = frame;
            exportBaseCursor = normalCursor;
        }

        int frameRate = Math.max(1, mchorse.bbs_mod.client.BBSRendering.getVideoFrameRate());

        return exportBaseCursor + (frame - exportBaseFrame) * (20F / frameRate);
    }
}
