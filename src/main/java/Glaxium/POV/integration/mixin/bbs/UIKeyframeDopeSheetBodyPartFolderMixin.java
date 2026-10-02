package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartFolderSheet;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps POV bodypart folders as UI-only rows rather than fake Boolean tracks. */
@Mixin(value = UIKeyframeDopeSheet.class, remap = false)
public class UIKeyframeDopeSheetBodyPartFolderMixin
{
    @Inject(method = "renderSheet", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideFolderTrack(
        UIContext context, BufferBuilder builder, Matrix4f matrix, Area area,
        UIKeyframeSheet sheet, int y, CallbackInfo info)
    {
        if (sheet instanceof PovBodyPartFolderSheet)
        {
            info.cancel();
        }
    }

    @Inject(method = "renderSheetKeyframeShapes", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideFolderKeyShapes(
        UIContext context, BufferBuilder builder, Matrix4f matrix, Area area,
        UIKeyframeSheet sheet, int y, CallbackInfo info)
    {
        if (sheet instanceof PovBodyPartFolderSheet)
        {
            info.cancel();
        }
    }

    @Inject(method = "getSheetIndent", at = @At("HEAD"), cancellable = true)
    private void bbsPov$alignBodyPartChildren(
        UIKeyframeSheet sheet, CallbackInfoReturnable<Integer> info)
    {
        /* Bodypart properties and hand pose overlays belong visually directly connected to the left
         * without an indentation gap. */
        if (sheet.id.startsWith("pose_overlay") || sheet.id.matches("[^/]+/(visible|lighting|transform|transform_overlay.*|texture|pose|pose_overlay.*|color|color_overlay.*|actions)"))
        {
            info.setReturnValue(0);
        }
        else if (sheet.id.matches("[^/]+/(bone:.*|pose\\.bones\\..*)"))
        {
            /* Same compact offset used by Hand Pose: no extra BodyPart-folder
             * indentation, only the actual model bone depth. */
            int depth = Math.max(0, sheet.getDepth() - 1);
            info.setReturnValue(4 + depth * 4);
        }
    }

    @Inject(method = "renderSectionKeyframes", at = @At("HEAD"), cancellable = true)
    private void bbsPov$hideSectionKeyframes(
        UIContext context, BufferBuilder builder, Matrix4f matrix, Area area, CallbackInfo info)
    {
        info.cancel();
    }
}
