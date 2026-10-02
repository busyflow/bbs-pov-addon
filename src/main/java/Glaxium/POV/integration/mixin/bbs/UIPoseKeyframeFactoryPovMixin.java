package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.PovItemPose;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/** Restricts the POV Hand Pose bone picker to the configured FP roots and descendants. */
@Mixin(value = UIPoseKeyframeFactory.class, remap = false)
public class UIPoseKeyframeFactoryPovMixin
{
    @Shadow public UIPoseKeyframeFactory.UIPoseFactoryEditor poseEditor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsPov$filterHandBones(
        mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue<Pose> track,
        UIKeyframes editor,
        CallbackInfo info)
    {
        UIKeyframeSheet sheet = track.sheet;

        if (sheet != null && sheet.channel != null && "pov_hand_item_pose".equals(sheet.channel.getId()))
        {
            this.poseEditor.setPose(track.getValue(), "");
            this.poseEditor.fillGroups(PovItemPose.BONES, false);
            return;
        }

        if (sheet == null)
        {
            return;
        }

        ModelForm form = sheet.getPoseForm();
        if (form == null && sheet.form instanceof ModelForm modelForm)
        {
            form = modelForm;
        }
        if (form == null && sheet.property != null && mchorse.bbs_mod.forms.FormUtils.getForm(sheet.property) instanceof ModelForm modelForm)
        {
            form = modelForm;
        }
        if (form == null)
        {
            return;
        }

        /* BodyPart sheets always have path prefixes (e.g. "sword/pose", "hat/pose_overlay").
         * BodyParts must show all of their own model's bones, not be restricted to hand bones. */
        if (sheet.id != null && sheet.id.contains("/"))
        {
            return;
        }

        String channelId = sheet.channel != null && sheet.channel.getId() != null ? sheet.channel.getId() : "";
        boolean isHandPose = false;

        if (UIPovHandEditor.isActive())
        {
            isHandPose = true;
        }
        else if (channelId.startsWith("pov_hand_"))
        {
            isHandPose = true;
        }
        else
        {
            UIFilmPanel panel = PovReplaySettings.getFilmPanel();
            if (panel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null)
            {
                UIPovEditor povEditor = access.bbsPov$getEditor();
                if (form == povEditor.getHandEditorForm() || (editor == povEditor.keyframeEditor.view && povEditor.isHandSection()))
                {
                    isHandPose = true;
                }
            }
        }

        if (!isHandPose)
        {
            return;
        }

        if (!(FormUtilsClient.getRenderer(form) instanceof ModelFormRenderer renderer))
        {
            return;
        }

        ModelInstance model = renderer.getModel();
        HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);

        if (model == null || handBones.depths().isEmpty())
        {
            return;
        }

        Set<String> hidden = new LinkedHashSet<>();

        for (String bone : model.getModel().getGroupKeysInHierarchyOrder())
        {
            if (!handBones.contains(bone))
            {
                hidden.add(bone);
            }
        }

        this.poseEditor.fillGroups(model.getModel(), model.getFlippedParts(), false, hidden);
    }
}
