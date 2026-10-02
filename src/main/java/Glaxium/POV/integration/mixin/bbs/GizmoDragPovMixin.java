package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.IGizmoDragFirstPerson;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GizmoDrag.class, remap = false)
public abstract class GizmoDragPovMixin implements IGizmoDragFirstPerson
{
    @Unique private Vector3f bbsPov$rotationPivot;

    @Override
    public void bbsPov$setRotationPivot(Vector3f pivot)
    {
        this.bbsPov$rotationPivot = pivot != null ? new Vector3f(pivot) : null;
    }

    @Override
    public Vector3f bbsPov$getRotationPivot()
    {
        return this.bbsPov$rotationPivot;
    }
}
