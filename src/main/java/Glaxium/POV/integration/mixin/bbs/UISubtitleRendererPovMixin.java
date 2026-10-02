package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.render.PovOverlayRenderer;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.ui.film.UISubtitleRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Adds actor POV overlays to the same deterministic screen-effect pass BBS uses for subtitles. */
@Mixin(value = UISubtitleRenderer.class, remap = false)
public abstract class UISubtitleRendererPovMixin
{
    @Inject(method = "renderSubtitles", at = @At("RETURN"))
    private static void bbsPov$renderHotbar(
        MatrixStack matrices,
        Batcher2D batcher,
        List<Subtitle> subtitles,
        CallbackInfo info
    )
    {
    }
}
