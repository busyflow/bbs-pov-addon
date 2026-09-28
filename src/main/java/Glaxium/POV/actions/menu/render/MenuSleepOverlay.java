package Glaxium.POV.actions.menu.render;

public final class MenuSleepOverlay {
   public static final int RGB = 1052704;
   public static final float MAX_ALPHA = 220.0F;

   private MenuSleepOverlay() {
   }

   public static float progress(int sleepTimer) {
      if (sleepTimer <= 0) {
         return 0.0F;
      } else {
         float timer = (float)sleepTimer;
         float progress = timer / 100.0F;
         if (progress > 1.0F) {
            progress = 1.0F - (timer - 100.0F) / 10.0F;
         }

         return Math.max(0.0F, Math.min(1.0F, progress));
      }
   }

   public static int color(float progress) {
      float clamped = Math.max(0.0F, Math.min(1.0F, progress));
      int alpha = (int)(220.0F * clamped);
      return alpha << 24 | 1052704;
   }
}
