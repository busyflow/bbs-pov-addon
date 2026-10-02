package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Automatically notifies the keyframe editor callback when selection transitions to null
 * so that the keyframe settings/properties menu is dismissed when all keyframes are deselected.
 */
@Mixin(value = UIKeyframes.class, remap = false)
public abstract class UIKeyframesPovMixin
{
    @Shadow private Runnable callback;
    @Shadow private IUIKeyframeGraph currentGraph;

    @Unique
    private boolean bbsPov$hadSelection;

    @Inject(method = "render", at = @At("HEAD"))
    private void bbsPov$autoCloseEditorOnDeselect(UIContext context, CallbackInfo info)
    {
        if (this.currentGraph != null)
        {
            boolean hasSelected = this.currentGraph.getSelected() != null;
            if (!hasSelected && this.bbsPov$hadSelection)
            {
                if (this.callback != null)
                {
                    this.callback.run();
                }
            }
            this.bbsPov$hadSelection = hasSelected;
        }
    }
}
