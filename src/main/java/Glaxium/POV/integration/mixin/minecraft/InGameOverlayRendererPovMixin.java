package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses in-game overlay effects (underwater, fire, in-wall suffocation) during BBS film editing and playback. */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererPovMixin
{
    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$hideUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$hideFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            if (client.player != null && (client.player.isSubmergedIn(net.minecraft.registry.tag.FluidTags.LAVA) || client.player.isInLava()))
            {
                return;
            }
            info.cancel();
        }
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$hideInWallOverlay(Sprite sprite, MatrixStack matrices, CallbackInfo info)
    {
        if (Glaxium.POV.utils.PovEffectSuppression.isBbsActive())
        {
            info.cancel();
        }
    }
}
