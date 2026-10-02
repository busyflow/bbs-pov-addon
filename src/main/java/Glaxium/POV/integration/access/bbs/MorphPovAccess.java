package Glaxium.POV.integration.access.bbs;

import mchorse.bbs_mod.forms.forms.Form;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface MorphPovAccess
{
    void bbsPov$setFormRaw(Form form);
}
