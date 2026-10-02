package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.gui.hud.ClientBossBar;

import java.util.Map;
import java.util.UUID;

public interface BossBarHudPovAccess
{
    Map<UUID, ClientBossBar> bbsPov$getBossBars();
}
