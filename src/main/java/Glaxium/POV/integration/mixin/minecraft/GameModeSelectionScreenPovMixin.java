package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.GameModeSelectionScreenPovAccess;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({GameModeSelectionScreen.class})
public class GameModeSelectionScreenPovMixin implements GameModeSelectionScreenPovAccess {
   private static Field FIELD;

   @Override
   public Object bbsPov$getGameMode() {
      try {
         if (FIELD == null) {
            for (Field f : GameModeSelectionScreen.class.getDeclaredFields()) {
               if (f.getType().isEnum() && !Modifier.isFinal(f.getModifiers())) {
                  f.setAccessible(true);
                  FIELD = f;
                  break;
               }
            }
         }

         if (FIELD != null) {
            return FIELD.get(this);
         }
      } catch (Exception var5) {
      }

      return null;
   }
}
