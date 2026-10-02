package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.DeathScreenPovAccess;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeathScreen.class)
public interface DeathScreenPovAccessor extends DeathScreenPovAccess
{
    @Accessor("message")
    Text bbsPov$getDeathMessage();

    @Accessor("scoreText")
    Text bbsPov$getScoreText();

    @Accessor("ticksSinceDeath")
    int bbsPov$getTicksSinceDeath();
}
