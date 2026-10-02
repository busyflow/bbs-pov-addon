package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.gui.recording.GuiSnapshotCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenPovMixin
{
    @Shadow private int scrollOffset;

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$captureStonecutterState(
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        CallbackInfo info)
    {
        GuiSnapshotCapture.updateStonecutter(this.scrollOffset);
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void bbsPov$captureStonecutterWheel(
        double mouseX,
        double mouseY,
        double amount,
        CallbackInfoReturnable<Boolean> info)
    {
        GuiSnapshotCapture.updateStonecutter(this.scrollOffset);
    }

    @Inject(method = "mouseDragged", at = @At("RETURN"))
    private void bbsPov$captureStonecutterScrollbar(
        double mouseX,
        double mouseY,
        int button,
        double deltaX,
        double deltaY,
        CallbackInfoReturnable<Boolean> info)
    {
        GuiSnapshotCapture.updateStonecutter(this.scrollOffset);
    }
}
