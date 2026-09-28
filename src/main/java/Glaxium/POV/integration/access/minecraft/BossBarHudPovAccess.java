package Glaxium.POV.integration.access.minecraft;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.hud.ClientBossBar;

public interface BossBarHudPovAccess {
   Map<UUID, ClientBossBar> bbsPov$getBossBars();
}
