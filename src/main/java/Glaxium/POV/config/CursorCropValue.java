package Glaxium.POV.config;

import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import org.joml.Vector4f;

/** Distinct settings value so the cursor crop can have its own settings UI. */
public class CursorCropValue extends ValueVector4f
{
    public CursorCropValue(String id)
    {
        super(id, new Vector4f());
    }
}
