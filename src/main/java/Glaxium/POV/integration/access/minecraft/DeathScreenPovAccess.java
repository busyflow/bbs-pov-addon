package Glaxium.POV.integration.access.minecraft;

import net.minecraft.text.Text;

/** Feature-facing accessor for DeathScreen private fields. */
public interface DeathScreenPovAccess
{
    Text bbsPov$getDeathMessage();

    Text bbsPov$getScoreText();

    int bbsPov$getTicksSinceDeath();
}
