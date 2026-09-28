package Glaxium.POV.recording;

public final class ActiveRecordingRange {
   private static final ThreadLocal<int[]> RANGE = new ThreadLocal<>();

   private ActiveRecordingRange() {
   }

   public static void set(int start, int end) {
      if (start >= 0 && end >= start) {
         RANGE.set(new int[]{start, end});
      } else {
         RANGE.remove();
      }
   }

   public static int[] get() {
      return RANGE.get();
   }

   public static boolean isActive() {
      return RANGE.get() != null;
   }

   public static void clear() {
      RANGE.remove();
   }
}
