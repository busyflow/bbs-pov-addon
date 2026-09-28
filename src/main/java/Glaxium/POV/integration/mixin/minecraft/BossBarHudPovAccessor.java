package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.BossBarHudPovAccess;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BossBarHud.class})
public interface BossBarHudPovAccessor extends BossBarHudPovAccess {
   @Accessor("bossBars")
   @Override
   Map<UUID, ClientBossBar> bbsPov$getBossBars();
}
