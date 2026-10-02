package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.GameModeSelectionScreenPovAccess;
import net.minecraft.client.gui.screen.GameModeSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import java.lang.reflect.Field;

@Mixin(GameModeSelectionScreen.class)
public class GameModeSelectionScreenPovMixin implements GameModeSelectionScreenPovAccess
{
    private static Field FIELD;

    @Override
    public Object bbsPov$getGameMode()
    {
        try
        {
            if (FIELD == null)
            {
                for (Field f : GameModeSelectionScreen.class.getDeclaredFields())
                {
                    if (f.getType().isEnum() && !java.lang.reflect.Modifier.isFinal(f.getModifiers()))
                    {
                        f.setAccessible(true);
                        FIELD = f;
                        break;
                    }
                }
            }
            if (FIELD != null)
            {
                return FIELD.get(this);
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
