package Glaxium.POV.actions.menu.schema;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;

public final class MenuTypeResolver {
   private MenuTypeResolver() {
   }

   public static String resolve(Screen screen) {
      if (screen instanceof GameMenuScreen) {
         return "game_menu";
      } else if (screen instanceof DeathScreen) {
         return "death";
      } else {
         return screen instanceof SleepingChatScreen ? "sleep" : null;
      }
   }

   public static String resolveLive(Screen screen, ClientPlayerEntity player) {
      String type = resolve(screen);
      if (type != null) {
         return type;
      } else {
         return player != null && player.getSleepTimer() > 0 ? "sleep" : null;
      }
   }
}
