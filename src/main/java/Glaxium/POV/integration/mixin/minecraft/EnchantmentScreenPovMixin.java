package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenPovMixin
{
    @Shadow private float pageTurningSpeed;
    @Shadow private float nextPageTurningSpeed;

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$captureEnchantment(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        EnchantmentScreen screen = (EnchantmentScreen) (Object) this;
        EnchantmentScreenHandler handler = screen.getScreenHandler();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        float open = MathHelper.lerp(client.getTickDelta(), this.pageTurningSpeed, this.nextPageTurningSpeed);
        GuiSnapshotCapture.updateEnchantment(
            handler.enchantmentPower,
            handler.enchantmentId,
            handler.enchantmentLevel,
            handler.getSeed(),
            player == null ? 0 : player.experienceLevel,
            player != null && player.getAbilities().creativeMode,
            open);
    }
}
