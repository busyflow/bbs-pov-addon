package Glaxium.POV.hud;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class HudMount {
   private HudMount() {
   }

   public static LivingEntity jumpingMount(PlayerEntity player) {
      if (player == null) {
         return null;
      } else {
         return player.getVehicle() instanceof LivingEntity living ? living : null;
      }
   }

   public static int heartSlots(LivingEntity mount) {
      if (mount != null && !mount.isRemoved()) {
         int slots = (int)(mount.getMaxHealth() + 0.5F) / 2;
         return Math.max(0, Math.min(30, slots));
      } else {
         return 0;
      }
   }
}
