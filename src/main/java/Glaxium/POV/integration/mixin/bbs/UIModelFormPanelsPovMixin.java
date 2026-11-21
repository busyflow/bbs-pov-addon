package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Filters out IK Chains, Physics Chains, and Bone Constraints panels from UIModelForm
 * when inside the POV Hand Editor, ensuring no empty gaps remain in the tab bar.
 */
@Mixin(value = UIModelForm.class, remap = false)
public abstract class UIModelFormPanelsPovMixin
{
    @SuppressWarnings("unchecked")
    @Redirect(
        method = "<init>",
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/forms/editors/forms/UIModelForm;registerPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;Lmchorse/bbs_mod/l10n/keys/IKey;Lmchorse/bbs_mod/ui/utils/icons/Icon;)Lmchorse/bbs_mod/ui/framework/elements/buttons/UIIcon;"
        )
    )
    private UIIcon bbsPov$filterModelPanels(UIModelForm self, UIElement panel, IKey tooltip, Icon icon)
    {
        if (UIPovHandEditor.isActive())
        {
            String name = panel.getClass().getSimpleName();

            if (name.equals("UIModelIKFormPanel") || name.equals("UIModelPhysicsFormPanel") || name.equals("UIModelConstraintsFormPanel"))
            {
                return null;
            }
        }

        return self.registerPanel((mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel<mchorse.bbs_mod.forms.forms.ModelForm>) panel, tooltip, icon);
    }
}
