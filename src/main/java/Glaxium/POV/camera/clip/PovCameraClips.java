package Glaxium.POV.camera.clip;

import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.replay.ReplayPovAccess;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public final class PovCameraClips {
   private PovCameraClips() {
   }

   public static PovCameraClip resolve(Film film, float filmTick) {
      if (film == null) {
         return null;
      } else {
         int tick = (int)Math.floor((double)filmTick);
         PovCameraClip result = null;
         int topLayer = Integer.MIN_VALUE;

         for (Clip clip : film.camera.getClips(tick)) {
            if (clip instanceof PovCameraClip) {
               PovCameraClip pov = (PovCameraClip)clip;
               if ((Boolean)pov.enabled.get() && (Integer)pov.layer.get() >= topLayer) {
                  result = pov;
                  topLayer = (Integer)pov.layer.get();
               }
            }
         }

         return result;
      }
   }

   public static Replay resolveReplay(Film film, float filmTick) {
      return resolveReplay(film, resolve(film, filmTick));
   }

   public static Replay resolveReplay(Film film, PovCameraClip clip) {
      if (film != null && clip != null) {
         List<Replay> replays = film.replays.getList();
         int index = (Integer)clip.selector.get();
         return index >= 0 && index < replays.size() ? replays.get(index) : null;
      } else {
         return null;
      }
   }

   public static int indexOfReplay(Film film, Replay replay) {
      return film != null && replay != null ? film.replays.getList().indexOf(replay) : -1;
   }

   public static boolean hasAny(Film film) {
      return film != null && !film.camera.getClips(PovCameraClip.class).isEmpty();
   }

   public static boolean isActive(UIFilmPanel panel) {
      return panel != null && resolve((Film)panel.getData(), (float)panel.getCursor()) != null;
   }

   public static void ensureLegacyClip(Film film) {
      if (film != null && !hasAny(film)) {
         List<Replay> replays = film.replays.getList();
         Replay source = film.getFirstPersonReplay();
         if (source == null) {
            for (Replay replay : replays) {
               if (PovReplaySettings.isOverlayEnabled(replay)) {
                  source = replay;
                  break;
               }
            }
         }

         if (source != null) {
            int selector = replays.indexOf(source);
            if (selector >= 0) {
               source.fp.set(false);
               if (source instanceof ReplayPovAccess access) {
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
         }
      }
   }

   private static int inferDuration(Film film) {
      int duration = Math.max(1, film.camera.calculateDuration());

      for (Replay replay : film.replays.getList()) {
         for (KeyframeChannel<?> channel : replay.keyframes.getChannels()) {
            duration = Math.max(duration, (int)Math.ceil(channel.getLength()) + 1);
         }

         duration = Math.max(duration, replay.actions.calculateDuration());
         if (replay.keyframes instanceof ReplayKeyframesPovAccess access && access.bbsPov$getActions() != null) {
            duration = Math.max(duration, access.bbsPov$getActions().calculateDuration());
         }
      }

      return Math.max(1, duration);
   }
}
