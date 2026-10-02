package wemppy.bbs_pov.client.mixin;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.film.clips.UIPOVClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import wemppy.bbs_pov.client.duck.IPovHardcore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UIPOVClip.class)
public abstract class UIPOVClipMixin
{
    @Unique
    private UIToggle hardcoreToggle;

    @Inject(method = "registerUI", at = @At("RETURN"))
    private void onRegisterUI(CallbackInfo ci)
    {
        UIPOVClip self = (UIPOVClip) (Object) this;
        if (self.clip instanceof IPovHardcore hc)
        {
            ValueBoolean val = hc.bbs_pov$getHardcoreLook();
            this.hardcoreToggle = new UIToggle(IKey.raw("Hardcore look"), (b) -> self.editor.editMultiple(val, (v) -> v.set(b.getValue())));
            this.hardcoreToggle.valueBinding(() -> this.hardcoreToggle.setValue(val.get()));
        }
    }

    @Inject(method = "registerPanels", at = @At("RETURN"))
    private void onRegisterPanels(CallbackInfo ci)
    {
        UIPOVClip self = (UIPOVClip) (Object) this;
        if (this.hardcoreToggle != null)
        {
            self.panels.add(this.hardcoreToggle);
        }
    }
}
