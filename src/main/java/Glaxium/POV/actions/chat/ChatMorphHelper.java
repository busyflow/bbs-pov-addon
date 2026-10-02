package Glaxium.POV.actions.chat;

import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.entity.player.PlayerEntity;

public final class ChatMorphHelper
{
    private ChatMorphHelper()
    {
    }

    public static String getPlayerMorphName(PlayerEntity player)
    {
        if (player == null)
        {
            return null;
        }

        try
        {
            Morph morph = Morph.getMorph(player);
            if (morph != null && morph.getForm() != null)
            {
                Form form = morph.getForm();
                if (form.name.get() != null && !form.name.get().trim().isEmpty())
                {
                    return form.name.get().trim();
                }
                if (form instanceof ModelForm modelForm)
                {
                    String model = modelForm.model.get();
                    if (model != null && !model.trim().isEmpty())
                    {
                        return model.trim();
                    }
                }
                if (form.getId() != null && !form.getId().trim().isEmpty())
                {
                    return form.getId().trim();
                }
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    public static String getReplayName(Replay replay)
    {
        if (replay == null)
        {
            return null;
        }

        if (replay.label.get() != null && !replay.label.get().trim().isEmpty())
        {
            return replay.label.get().trim();
        }
        if (replay.nameTag.get() != null && !replay.nameTag.get().trim().isEmpty())
        {
            return replay.nameTag.get().trim();
        }
        if (replay.form.get() != null)
        {
            Form form = replay.form.get();
            if (form.name.get() != null && !form.name.get().trim().isEmpty())
            {
                return form.name.get().trim();
            }
            if (form instanceof ModelForm modelForm)
            {
                String model = modelForm.model.get();
                if (model != null && !model.trim().isEmpty())
                {
                    return model.trim();
                }
            }
            if (form.getId() != null && !form.getId().trim().isEmpty())
            {
                return form.getId().trim();
            }
        }
        return null;
    }

    public static String getActiveReplayName()
    {
        try
        {
            Glaxium.POV.recording.PovRecordingSession session = Glaxium.POV.recording.PovRecordingSession.getCurrent();
            if (session != null)
            {
                Replay replay = session.getReplay();
                if (replay != null)
                {
                    String name = getReplayName(replay);
                    if (name != null && !name.isEmpty())
                    {
                        return name;
                    }
                }
            }

            Glaxium.POV.playback.PovPlaybackContext.Frame frame = Glaxium.POV.playback.PovPlaybackContext.getActive();
            if (frame != null && frame.replay() != null)
            {
                String name = getReplayName(frame.replay());
                if (name != null && !name.isEmpty())
                {
                    return name;
                }
            }

            UIFilmPanel panel = PovReplaySettings.getFilmPanel();
            if (panel != null && panel.replayEditor != null)
            {
                Replay replay = panel.replayEditor.getReplay();
                return getReplayName(replay);
            }
        }
        catch (Throwable ignored)
        {
        }

        return null;
    }

    public static java.util.List<String> getAllFilmReplayNames()
    {
        java.util.List<String> names = new java.util.ArrayList<>();
        try
        {
            mchorse.bbs_mod.film.Film film = null;
            Glaxium.POV.recording.PovRecordingSession session = Glaxium.POV.recording.PovRecordingSession.getCurrent();
            if (session != null)
            {
                film = session.getFilm();
            }
            if (film == null)
            {
                Glaxium.POV.playback.PovPlaybackContext.Frame frame = Glaxium.POV.playback.PovPlaybackContext.getActive();
                if (frame != null)
                {
                    film = frame.film();
                }
            }
            if (film == null)
            {
                UIFilmPanel panel = PovReplaySettings.getFilmPanel();
                if (panel != null)
                {
                    film = panel.getData();
                }
            }
            if (film != null && film.replays != null)
            {
                for (Replay replay : film.replays.getList())
                {
                    String name = getReplayName(replay);
                    if (name != null && !name.isEmpty() && !names.contains(name))
                    {
                        names.add(name);
                    }
                }
            }
        }
        catch (Throwable ignored)
        {
        }
        return names;
    }
}
