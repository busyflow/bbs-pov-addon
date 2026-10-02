package Glaxium.POV.camera.clip;

import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;

/** Central resolver for POV camera clips and their replay sources. */
public final class PovCameraClips
{
    private PovCameraClips()
    {
    }

    public static double getStandingEyeHeight(IEntity entity, Form form)
    {
        if (form != null && form.hitbox.get())
        {
            float height = form.hitboxHeight.get();
            float eyeRatio = form.hitboxEyeHeight.get();
            if (eyeRatio <= 0.0F)
            {
                eyeRatio = 0.9F;
            }
            return (double) (height * eyeRatio);
        }
        return entity != null ? entity.getEyeHeight() : 1.62D;
    }

    public static double getSneakingEyeHeight(IEntity entity, Form form)
    {
        if (form != null && form.hitbox.get())
        {
            float height = form.hitboxHeight.get() * form.hitboxSneakMultiplier.get();
            float eyeRatio = form.hitboxEyeHeight.get();
            if (eyeRatio <= 0.0F)
            {
                eyeRatio = 0.9F;
            }
            return (double) (height * eyeRatio);
        }
        return 1.27D;
    }

    public static boolean isSneakingAt(Replay replay, IEntity entity, int tick)
    {
        if (replay != null && replay.keyframes != null && replay.keyframes.sneaking != null && !replay.keyframes.sneaking.isEmpty())
        {
            int looping = replay.looping.get();
            int t = looping > 0 ? ((tick % looping) + looping) % looping : tick;
            return replay.keyframes.sneaking.interpolate((float) t) > 0.5D;
        }
        return entity != null && entity.isSneaking();
    }

    public static double getSmoothPovEyeHeight(Replay replay, IEntity entity, Form form, float replayTick)
    {
        double standingEye = getStandingEyeHeight(entity, form);
        double sneakingEye = getSneakingEyeHeight(entity, form);

        if (Math.abs(standingEye - sneakingEye) < 1e-5)
        {
            return standingEye;
        }

        int curTick = (int) Math.floor(replayTick);
        float frac = replayTick - curTick;

        int startTick = curTick - 8;
        double sim = isSneakingAt(replay, entity, startTick) ? sneakingEye : standingEye;

        for (int t = startTick + 1; t <= curTick; t++)
        {
            double target = isSneakingAt(replay, entity, t) ? sneakingEye : standingEye;
            sim += (target - sim) * 0.5D;
        }

        double prev = sim;
        double nextTarget = isSneakingAt(replay, entity, curTick + 1) ? sneakingEye : standingEye;
        double next = sim + (nextTarget - sim) * 0.5D;

        return mchorse.bbs_mod.utils.interps.Lerps.lerp(prev, next, frac);
    }

    public static double getPovEyeHeight(IEntity entity)
    {
        if (entity == null)
        {
            return 1.62D;
        }

        return getPovEyeHeight(entity, entity.getForm());
    }

    public static double getPovEyeHeight(IEntity entity, Form form)
    {
        return getSmoothPovEyeHeight(null, entity, form, 0F);
    }

    public static PovCameraClip resolve(Film film, float filmTick)
    {
        if (film == null)
        {
            return null;
        }

        int tick = (int) Math.floor(filmTick);
        PovCameraClip result = null;
        int topLayer = Integer.MIN_VALUE;

        for (Clip clip : film.camera.getClips(tick))
        {
            if (clip instanceof PovCameraClip pov
                && pov.enabled.get()
                && pov.layer.get() >= topLayer)
            {
                result = pov;
                topLayer = pov.layer.get();
            }
        }

        return result;
    }

    public static Replay resolveReplay(Film film, float filmTick)
    {
        return resolveReplay(film, resolve(film, filmTick));
    }

    public static Replay resolveReplay(Film film, PovCameraClip clip)
    {
        if (film == null || clip == null)
        {
            return null;
        }

        List<Replay> replays = film.replays.getList();
        int index = clip.selector.get();

        return index >= 0 && index < replays.size() ? replays.get(index) : null;
    }

    public static int indexOfReplay(Film film, Replay replay)
    {
        return film == null || replay == null ? -1 : film.replays.getList().indexOf(replay);
    }

    public static boolean hasAny(Film film)
    {
        return film != null && !film.camera.getClips(PovCameraClip.class).isEmpty();
    }

    public static boolean isActive(UIFilmPanel panel)
    {
        return panel != null && resolve((Film) panel.getData(), panel.getCursor()) != null;
    }

    /**
     * Old projects stored POV intent on replay.fp / bbs_pov_enabled. Convert it
     * once into a normal timeline clip so removing the old replay UI does not
     * silently destroy existing POV films.
     */
    public static void ensureLegacyClip(Film film)
    {
        if (film == null || hasAny(film))
        {
            return;
        }

        List<Replay> replays = film.replays.getList();
        Replay source = film.getFirstPersonReplay();

        if (source == null)
        {
            for (Replay replay : replays)
            {
                if (PovReplaySettings.isOverlayEnabled(replay))
                {
                    source = replay;
                    break;
                }
            }
        }

        if (source == null)
        {
            return;
        }

        int selector = replays.indexOf(source);

        if (selector < 0)
        {
            return;
        }

        source.fp.set(false);
        if (source instanceof Glaxium.POV.replay.ReplayPovAccess access)
        {
            access.bbsPov$getOverlayEnabled().set(false);
        }

        PovCameraClip clip = new PovCameraClip();
        clip.tick.set(0);
        clip.duration.set(inferDuration(film));
        clip.layer.set(Math.max(0, film.camera.getTopLayer() + 1));
        clip.selector.set(selector);
        clip.hands.set(true);
        clip.hud.set(true);
        clip.crosshair.set(true);
        clip.actions.set(true);
        clip.cursor.set(true);
        clip.headLook.set(true);
        film.camera.addClip(clip);
        film.camera.sync();
    }

    private static int inferDuration(Film film)
    {
        int duration = Math.max(1, film.camera.calculateDuration());

        for (Replay replay : film.replays.getList())
        {
            for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
            {
                duration = Math.max(duration, (int) Math.ceil(channel.getLength()) + 1);
            }

            duration = Math.max(duration, replay.actions.calculateDuration());

            if (replay.keyframes instanceof ReplayKeyframesPovAccess access
                && access.bbsPov$getActions() != null)
            {
                duration = Math.max(duration, access.bbsPov$getActions().calculateDuration());
            }
        }

        return Math.max(1, duration);
    }
}
