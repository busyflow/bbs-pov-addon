package Glaxium.POV.config;

import mchorse.bbs_mod.settings.values.core.ValueLink;

/** Distinct settings value so the cursor texture can have its own settings UI with a remove button. */
public class CursorTextureValue extends ValueLink
{
    public CursorTextureValue(String id)
    {
        super(id, null);
    }
}
