package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartGizmoDrag;
import mchorse.bbs_mod.ui.framework.elements.input.drag.ArcballDrag;
import mchorse.bbs_mod.ui.framework.elements.input.drag.DragContext;
import mchorse.bbs_mod.ui.framework.elements.input.drag.DragStrategy;
import mchorse.bbs_mod.ui.framework.elements.input.drag.DragStrategyFactory;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.utils.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DragStrategyFactory.class, remap = false)
public class BodyPartRotationStrategyPovMixin
{
    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$freeBodyPartRotation(
        DragContext context, TransformOp operation, Axis axis, Axis axis2,
        DragStrategyFactory.Variant variant,
        CallbackInfoReturnable<DragStrategy> info)
    {
        if (context.drag() instanceof PovBodyPartGizmoDrag
            && operation == TransformOp.ROTATE
            && variant == DragStrategyFactory.Variant.TRACKBALL)
        {
            /* Native arcball provides a full spatial turn from the first
             * gesture. Fixed yaw-then-pitch trackball keeps Z at zero from an
             * identity pose and can wind it by whole turns at Euler poles. */
            info.setReturnValue(new ArcballDrag(context));
        }
    }
}
