package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.config.PovSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Batcher2D.class, remap = false)
public abstract class Batcher2DPovMixin
{
    @Inject(method = "icon(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V", at = @At("HEAD"), cancellable = true)
    private void bbsPov$onIcon(Icon icon, int color, float x, float y, float ax, float ay, CallbackInfo info)
    {
        if (icon == Icons.CURSOR && PovSettings.cursorTexture != null && PovSettings.cursorTexture.get() != null)
        {
            Batcher2D batcher = (Batcher2D) (Object) this;
            x -= icon.w * ax;
            y -= icon.h * ay;

            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(x, y, 0);
            float scale = PovSettings.getCursorDefaultScale();
            batcher.getContext().getMatrices().scale(scale, scale, 1F);
            PovSettings.renderCursor(batcher);
            batcher.flush();
            batcher.getContext().getMatrices().pop();
            info.cancel();
        }
    }
}
