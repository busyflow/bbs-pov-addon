package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelConstraintsFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelIKFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelPhysicsFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   value = {UIModelForm.class},
   remap = false
)
public abstract class UIModelFormPanelsPovMixin {
   @Redirect(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/ui/forms/editors/forms/UIModelForm;registerPanel(Lmchorse/bbs_mod/ui/framework/elements/UIElement;Lmchorse/bbs_mod/l10n/keys/IKey;Lmchorse/bbs_mod/ui/utils/icons/Icon;)Lmchorse/bbs_mod/ui/framework/elements/buttons/UIIcon;"
      )
   )
   private UIIcon bbsPov$filterModelPanels(UIModelForm self, UIElement panel, IKey tooltip, Icon icon) {
      return !UIPovHandEditor.isActive()
            || !(panel instanceof UIModelIKFormPanel) && !(panel instanceof UIModelPhysicsFormPanel) && !(panel instanceof UIModelConstraintsFormPanel)
         ? self.registerPanel((UIFormPanel)panel, tooltip, icon)
         : null;
   }
}
