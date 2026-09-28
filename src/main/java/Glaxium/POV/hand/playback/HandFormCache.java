package Glaxium.POV.hand.playback;

import java.util.Map;
import java.util.WeakHashMap;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ModelForm;

public final class HandFormCache {
   private static final Map<Replay, ModelForm> FORMS = new WeakHashMap<>();
   private static int lastPlaybackTick = Integer.MIN_VALUE;
   private static Replay lastPlaybackReplay = null;

   private HandFormCache() {
   }

   public static ModelForm get(Replay replay) {
      return FORMS.computeIfAbsent(replay, ignored -> new ModelForm());
   }

   public static void updateIfNeeded(ModelForm form, IEntity entity, Replay replay, float tick, boolean isPlaying) {
      int currentTick = (int)Math.floor((double)tick);
      boolean isInitial = lastPlaybackTick == Integer.MIN_VALUE || replay != lastPlaybackReplay;
      if (isInitial) {
         lastPlaybackReplay = replay;
         lastPlaybackTick = currentTick;
         form.update(entity);
      } else if (isPlaying && currentTick != lastPlaybackTick) {
         lastPlaybackTick = currentTick;
         form.update(entity);
      }
   }
}
