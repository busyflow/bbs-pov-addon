package Glaxium.POV.integration.access.minecraft;

import net.minecraft.text.Text;

public interface DeathScreenPovAccess {
   Text bbsPov$getDeathMessage();

   Text bbsPov$getScoreText();

   int bbsPov$getTicksSinceDeath();
}
