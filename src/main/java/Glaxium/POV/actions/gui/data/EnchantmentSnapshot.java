package Glaxium.POV.actions.gui.data;

import net.minecraft.util.math.MathHelper;

public final class EnchantmentSnapshot {
   public final String offers;
   public final int seed;
   public final int playerLevel;
   public final boolean creative;
   public final float bookOpen;

   public EnchantmentSnapshot(String offers, int seed, int playerLevel, boolean creative, float bookOpen) {
      this.offers = offers == null ? "" : offers;
      this.seed = seed;
      this.playerLevel = Math.max(0, playerLevel);
      this.creative = creative;
      this.bookOpen = MathHelper.clamp(bookOpen, 0.0F, 1.0F);
   }

   public static String pack(int[] enchantPower, int[] enchantId, int[] enchantLevel) {
      StringBuilder packed = new StringBuilder();

      for (int i = 0; i < 3; i++) {
         if (i > 0) {
            packed.append(';');
         }

         packed.append(at(enchantPower, i, 0)).append(',').append(at(enchantId, i, -1)).append(',').append(at(enchantLevel, i, -1));
      }

      return packed.toString();
   }

   public static int[][] parse(String packed) {
      int[][] offers = new int[][]{{0, -1, -1}, {0, -1, -1}, {0, -1, -1}};
      if (packed != null && !packed.isEmpty()) {
         String[] rows = packed.split(";", -1);

         for (int i = 0; i < Math.min(3, rows.length); i++) {
            String[] parts = rows[i].split(",", -1);
            offers[i][0] = parseInt(parts, 0, 0);
            offers[i][1] = parseInt(parts, 1, -1);
            offers[i][2] = parseInt(parts, 2, -1);
         }

         return offers;
      } else {
         return offers;
      }
   }

   private static int at(int[] values, int index, int fallback) {
      return values != null && index >= 0 && index < values.length ? values[index] : fallback;
   }

   private static int parseInt(String[] parts, int index, int fallback) {
      if (parts != null && index >= 0 && index < parts.length) {
         try {
            return Integer.parseInt(parts[index].trim());
         } catch (NumberFormatException var4) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }
}
