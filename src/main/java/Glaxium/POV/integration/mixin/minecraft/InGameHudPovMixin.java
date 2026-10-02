package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.InGameHudVignettePovAccess;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Minecraft's real F1 flag while retaining the HUD callback that draws POV data. */
@Mixin(InGameHud.class)
public abstract class InGameHudPovMixin implements InGameHudVignettePovAccess
{
    @Shadow public float vignetteDarkness;
    @Shadow public float spyglassScale;

    @Shadow public abstract void renderVignetteOverlay(DrawContext context, Entity entity);

    @Override
    public void bbsPov$renderVignetteOverlay(DrawContext context, Entity entity)
    {
        this.renderVignetteOverlay(context, entity);
    }

    @Override
    public float bbsPov$getVignetteDarkness()
    {
        return this.vignetteDarkness;
    }

    @Override
    public float bbsPov$getSpyglassScale()
    {
        return this.spyglassScale;
    }

    @Unique private boolean bbsPov$oldHudHidden;
    @Unique private boolean bbsPov$changedHudHidden;

    @Unique
    private static boolean bbsPov$shouldSuppressScreenEffects()
    {
        return Glaxium.POV.utils.PovEffectSuppression.isBbsActive();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$enterPlaybackF1(DrawContext context, float tickDelta, CallbackInfo info)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean f4Recording = mchorse.bbs_mod.BBSModClient.getVideoRecorder() != null
            && mchorse.bbs_mod.BBSModClient.getVideoRecorder().isRecording();
        boolean liveOverlay = f4Recording
            && ((client.currentScreen != null && !(client.currentScreen instanceof mchorse.bbs_mod.ui.framework.UIScreen))
                || (client.player != null && client.player.getSleepTimer() > 0));
        if (PovPlaybackContext.getActive() != null || liveOverlay)
        {
            this.bbsPov$oldHudHidden = client.options.hudHidden;
            this.bbsPov$changedHudHidden = true;
            client.options.hudHidden = true;
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$skipHudVignetteAfterOverlay(DrawContext context, Entity entity, CallbackInfo info)
    {
        if (bbsPov$shouldSuppressScreenEffects())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$skipHudPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo info)
    {
        if (bbsPov$shouldSuppressScreenEffects())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$skipHudSpyglassOverlay(DrawContext context, float scale, CallbackInfo info)
    {
        if (bbsPov$shouldSuppressScreenEffects())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$skipHudCustomOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo info)
    {
        if (bbsPov$shouldSuppressScreenEffects())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$skipHudStatusEffectOverlay(DrawContext context, CallbackInfo info)
    {
        if (bbsPov$shouldSuppressScreenEffects())
        {
            info.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void bbsPov$leavePlaybackF1(DrawContext context, float tickDelta, CallbackInfo info)
    {
        if (this.bbsPov$changedHudHidden)
        {
            MinecraftClient.getInstance().options.hudHidden = this.bbsPov$oldHudHidden;
            this.bbsPov$changedHudHidden = false;
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideCrosshair(DrawContext context, CallbackInfo info)
    {
        if (MinecraftClient.getInstance().currentScreen != null || PovPlaybackContext.getActive() != null)
        {
            info.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideStatusEffectOverlay(DrawContext context, CallbackInfo info)
    {
        if (PovPlaybackContext.getActive() != null
            || PovReplaySettings.getFilmPanel() != null)
        {
            info.cancel();
        }
    }
}
