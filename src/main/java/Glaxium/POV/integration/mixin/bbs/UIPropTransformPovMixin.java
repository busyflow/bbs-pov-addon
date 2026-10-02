package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hud.editor.IUIPropTransform2DLayout;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = UIPropTransform.class, remap = false)
public abstract class UIPropTransformPovMixin extends UIElement implements IUIPropTransform2DLayout
{
    @Unique private boolean bbsPov$is2DLayout;

    @Override
    public void bbsPov$set2DLayout(boolean layout)
    {
        this.bbsPov$is2DLayout = layout;
    }

    @Override
    public boolean bbsPov$is2DLayout()
    {
        return this.bbsPov$is2DLayout;
    }
}
