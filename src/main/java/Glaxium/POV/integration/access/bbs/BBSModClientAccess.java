package Glaxium.POV.integration.access.bbs;

import Glaxium.POV.integration.mixin.bbs.BBSModClientAccessor;
import net.minecraft.client.option.KeyBinding;

public final class BBSModClientAccess {
   private BBSModClientAccess() {
   }

   public static KeyBinding bbsPov$getKeyPlayFilm() {
      return BBSModClientAccessor.bbsPov$getKeyPlayFilm();
   }

   public static KeyBinding bbsPov$getKeyRecordVideo() {
      return BBSModClientAccessor.bbsPov$getKeyRecordVideo();
   }
}
