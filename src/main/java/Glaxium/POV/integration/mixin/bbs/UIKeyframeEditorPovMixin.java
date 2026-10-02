package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.utils.UITimelinePanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.utils.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets BBS's native Film gizmo resolve custom channels. */
@Mixin(value = UIKeyframeEditor.class, remap = false)
public abstract class UIKeyframeEditorPovMixin extends UITimelinePanel
{
    @Shadow public UIKeyframeFactory<?> editor;

    @Inject(method = "getBone", at = @At("HEAD"), cancellable = true, require = 0)
    private void bbsPov$getParentPoseBone(CallbackInfoReturnable<Pair<String, TransformSpace>> info)
    {
        if (this.editor instanceof UIPoseKeyframeFactory poseFactory)
        {
            UIKeyframeSheet sheet = poseFactory.getSheet();
            String channel = sheet != null && sheet.channel != null ? sheet.channel.getId() : "";

            if ("pov_hand_pose".equals(channel) || "pov_hand_item_pose".equals(channel))
            {
                String bone = poseFactory.poseEditor.groups.list.getCurrentFirst();

                if (bone != null && !bone.isBlank())
                {
                    info.setReturnValue(new Pair<>(bone, poseFactory.poseEditor.transform.getSpace()));
                }
            }
        }
    }
}
