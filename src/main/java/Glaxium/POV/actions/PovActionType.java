package Glaxium.POV.actions;

import java.util.Locale;

public enum PovActionType {
   GUI("gui", "GUI"),
   MENU("menu", "Menu"),
   CAMERA_SHAKE("camera_shake", "Camera Shake"),
   PARTICLE_EFFECT("particle_effect", "Particle Effect"),
   BOSS_BARS("boss_bars", "Boss Bars"),
   STATUS_EFFECTS("status_effects", "Status Effects"),
   TOASTS("toasts", "Toasts"),
   CHAT("chat", "Chat"),
   SCREEN_EFFECT("screen_effect", "Screen Effect");

   public final String id;
   public final String title;

   private PovActionType(String id, String title) {
      this.id = id;
      this.title = title;
   }

   public int seedLayer() {
      return switch (this) {
         case GUI -> 0;
         case MENU -> 1;
         case CAMERA_SHAKE -> 2;
         case PARTICLE_EFFECT -> 6;
         case BOSS_BARS -> 12;
         case STATUS_EFFECTS -> 4;
         case TOASTS -> 7;
         case CHAT -> 3;
         case SCREEN_EFFECT -> 5;
      };
   }

   public static PovActionType fromId(String id) {
      if (id != null) {
         if ("particle".equals(id.toLowerCase(Locale.ROOT)) || "eating_effect".equals(id.toLowerCase(Locale.ROOT))) {
            return PARTICLE_EFFECT;
         }

         for (PovActionType type : values()) {
            if (type.id.equals(id.toLowerCase(Locale.ROOT))) {
               return type;
            }
         }
      }

      return GUI;
   }
}
