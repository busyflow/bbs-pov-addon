package Glaxium.POV.actions.chat;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.recording.PovRecordingSession;
import Glaxium.POV.replay.PovReplaySettings;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.minecraft.entity.player.PlayerEntity;

public final class ChatMorphHelper {
   private ChatMorphHelper() {
   }

   public static String getPlayerMorphName(PlayerEntity player) {
      if (player == null) {
         return null;
      } else {
         try {
            Morph morph = Morph.getMorph(player);
            if (morph != null && morph.getForm() != null) {
               Form form = morph.getForm();
               if (form.name.get() != null && !((String)form.name.get()).trim().isEmpty()) {
                  return ((String)form.name.get()).trim();
               }

               if (form instanceof ModelForm modelForm) {
                  String model = (String)modelForm.model.get();
                  if (model != null && !model.trim().isEmpty()) {
                     return model.trim();
                  }
               }

               if (form.getId() != null && !form.getId().trim().isEmpty()) {
                  return form.getId().trim();
               }
            }
         } catch (Throwable var5) {
         }

         return null;
      }
   }

   public static String getReplayName(Replay replay) {
      if (replay == null) {
         return null;
      } else if (replay.label.get() != null && !((String)replay.label.get()).trim().isEmpty()) {
         return ((String)replay.label.get()).trim();
      } else if (replay.nameTag.get() != null && !((String)replay.nameTag.get()).trim().isEmpty()) {
         return ((String)replay.nameTag.get()).trim();
      } else {
         if (replay.form.get() != null) {
            Form form = (Form)replay.form.get();
            if (form.name.get() != null && !((String)form.name.get()).trim().isEmpty()) {
               return ((String)form.name.get()).trim();
            }

            if (form instanceof ModelForm modelForm) {
               String model = (String)modelForm.model.get();
               if (model != null && !model.trim().isEmpty()) {
                  return model.trim();
               }
            }

            if (form.getId() != null && !form.getId().trim().isEmpty()) {
               return form.getId().trim();
            }
         }

         return null;
      }
   }

   public static String getActiveReplayName() {
      try {
         PovRecordingSession session = PovRecordingSession.getCurrent();
         if (session != null) {
            Replay replay = session.getReplay();
            if (replay != null) {
               String name = getReplayName(replay);
               if (name != null && !name.isEmpty()) {
                  return name;
               }
            }
         }

         PovPlaybackContext.Frame frame = PovPlaybackContext.getActive();
         if (frame != null && frame.replay() != null) {
            String name = getReplayName(frame.replay());
            if (name != null && !name.isEmpty()) {
               return name;
            }
         }

         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         if (panel != null && panel.replayEditor != null) {
            Replay replay = panel.replayEditor.getReplay();
            return getReplayName(replay);
         }
      } catch (Throwable var4) {
      }

      return null;
   }

   public static List<String> getAllFilmReplayNames() {
      List<String> names = new ArrayList<>();

      try {
         Film film = null;
         PovRecordingSession session = PovRecordingSession.getCurrent();
         if (session != null) {
            film = session.getFilm();
         }

         if (film == null) {
            PovPlaybackContext.Frame frame = PovPlaybackContext.getActive();
            if (frame != null) {
               film = frame.film();
            }
         }

         if (film == null) {
            UIFilmPanel panel = PovReplaySettings.getFilmPanel();
            if (panel != null) {
               film = (Film)panel.getData();
            }
         }

         if (film != null && film.replays != null) {
            for (Replay replay : film.replays.getList()) {
               String name = getReplayName(replay);
               if (name != null && !name.isEmpty() && !names.contains(name)) {
                  names.add(name);
               }
            }
         }
      } catch (Throwable var6) {
      }

      return names;
   }
}
