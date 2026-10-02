package Glaxium.POV.actions.menu.schema;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;

/** Maps vanilla screens to Menu type ids. Nested Options/etc return null. */
public final class MenuTypeResolver
{
    private MenuTypeResolver()
    {
    }

    public static String resolve(Screen screen)
    {
        if (screen instanceof GameMenuScreen menu && menu.shouldShowMenu())
        {
            return "game_menu";
        }
        if (screen instanceof DeathScreen)
        {
            return "death";
        }
        if (screen instanceof SleepingChatScreen)
        {
            return "sleep";
        }
        return null;
    }

    public static String resolveLive(Screen screen, net.minecraft.client.network.ClientPlayerEntity player)
    {
        String type = resolve(screen);
        if (type != null)
        {
            return type;
        }
        if (player != null && player.getSleepTimer() > 0)
        {
            return "sleep";
        }
        return null;
    }
}
