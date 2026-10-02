package Glaxium.POV.integration.mixin.bbs;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.keyframes.factories.TransformKeyframeFactory;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Migrates Layout keyframes written by older BBS-POV builds to BBS's native
 * Transform value. Native Transform keyframes are maps; the old Layout value
 * is the only Transform input stored as a Vector4 list. */
@Mixin(value = TransformKeyframeFactory.class, remap = false)
public abstract class TransformKeyframeFactoryPovMixin
{
    @Inject(method = "fromData", at = @At("HEAD"), cancellable = true)
    private void bbsPov$readLegacyLayout(
        BaseType data,
        CallbackInfoReturnable<Transform> info)
    {
        if (data == null || !data.isList())
        {
            return;
        }

        Vector4f old = DataStorageUtils.vector4fFromData(data.asList());
        Transform transform = new Transform();

        /* The original generic Vector4 editor created (0, 0, 0, 1), while the
         * intended Layout identity was (0, 0, 1, 0). Handle that default without
         * turning a previously untouched HUD invisible. */
        boolean genericDefault = old.z == 0F && old.w == 1F;
        float scale = genericDefault ? 1F : old.z;
        float rotation = genericDefault ? 0F : (float) Math.toRadians(old.w);

        transform.translate.set(old.x, old.y, 0F);
        transform.scale.set(scale, scale, 1F);
        transform.rotate.set(0F, 0F, rotation);
        info.setReturnValue(transform);
    }
}
