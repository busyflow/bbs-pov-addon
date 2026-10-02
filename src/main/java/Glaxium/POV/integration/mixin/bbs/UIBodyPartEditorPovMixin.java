package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.forms.editors.UIBodyPartEditor;
import mchorse.bbs_mod.utils.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UIBodyPartEditor.class, remap = false)
public class UIBodyPartEditorPovMixin
{
    @Inject(method = "pickBone", at = @At("HEAD"), cancellable = true)
    private void bbsPov$restrictViewportPickBone(Pair<Form, String> pair, CallbackInfoReturnable<Boolean> info)
    {
        if (UIPovHandEditor.isActive() && pair != null)
        {
            UIPovHandEditor editor = UIPovHandEditor.getActive();
            if (editor != null && pair.a == editor.getRootForm() && editor.getRootForm() instanceof ModelForm rootForm)
            {
                ModelInstance instance = ModelFormRenderer.getModel(rootForm);
                if (instance != null)
                {
                    HandBoneUtils.HandBones handBones = HandBoneUtils.collect(instance);
                    if (!handBones.contains(pair.b))
                    {
                        info.setReturnValue(false);
                    }
                }
            }
        }
    }
}
