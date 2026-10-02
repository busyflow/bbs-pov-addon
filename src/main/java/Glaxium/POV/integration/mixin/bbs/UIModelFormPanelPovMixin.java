package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Automatically filters the Pose Editor bone list to only show POV hand bones
 * on the root hand form, while exposing all model bones for body parts.
 */
@Mixin(value = UIModelFormPanel.class, remap = false)
public class UIModelFormPanelPovMixin
{
    @Shadow public UIModelPoseEditor poseEditor;

    @Inject(method = "startEdit(Lmchorse/bbs_mod/forms/forms/ModelForm;)V", at = @At("TAIL"))
    private void bbsPov$filterPoseEditor(ModelForm form, CallbackInfo info)
    {
        if (UIPovHandEditor.isActive())
        {
            UIPovHandEditor editor = UIPovHandEditor.getActive();
            boolean isRoot = editor != null && form == editor.getRootForm();

            if (isRoot)
            {
                if (((UIModelFormPanel) (Object) this).shapeKeysSection != null)
                {
                    ((UIModelFormPanel) (Object) this).shapeKeysSection.removeFromParent();
                    ((UIModelFormPanel) (Object) this).options.resize();
                }
                UIPovHandEditor.filterModelPoseEditor(this.poseEditor, form);
            }
            else
            {
                ModelInstance model = ModelFormRenderer.getModel(form);
                if (model != null && model.getModel() != null)
                {
                    this.poseEditor.fillGroups(model.getModel(), model.getFlippedParts(), false, null);
                }
            }
        }
    }
}

