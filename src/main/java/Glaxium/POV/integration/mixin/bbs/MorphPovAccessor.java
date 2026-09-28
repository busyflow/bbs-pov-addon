package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.MorphPovAccess;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {Morph.class},
   remap = false
)
public interface MorphPovAccessor extends MorphPovAccess {
   @Accessor("form")
   @Override
   void bbsPov$setFormRaw(Form var1);
}
