package Glaxium.POV.hand;

import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.joml.Matrix4f;

import java.util.List;

/** The two virtual bones exposed by the POV Item Pose keyframe. */
public final class PovItemPose
{
    public static final String MAIN_HAND = "Main Hand Item";
    public static final String OFF_HAND = "Off Hand Item";
    public static final List<String> BONES = List.of(MAIN_HAND, OFF_HAND);

    private PovItemPose()
    {}

    public static String bone(boolean leftHanded, Arm mainArm)
    {
        return leftHanded == (mainArm == Arm.LEFT) ? MAIN_HAND : OFF_HAND;
    }

    public static PoseTransform transform(Pose pose, String bone)
    {
        PoseTransform transform = pose == null ? null : pose.transforms.get(bone);

        return transform == null ? new PoseTransform() : transform;
    }

    /** Match BBS model-pose units: translation is measured in model pixels. */
    public static void apply(MatrixStack matrices, PoseTransform transform)
    {
        matrices.translate(
            -transform.translate.x / 16F,
            transform.translate.y / 16F,
            transform.translate.z / 16F);
        matrices.multiply(transform.createRotation());
        matrices.scale(transform.scale.x, transform.scale.y, transform.scale.z);
    }

    public static Matrix4f createMatrix(Transform transform)
    {
        return new Matrix4f()
            .translate(
                -transform.translate.x / 16F,
                transform.translate.y / 16F,
                transform.translate.z / 16F)
            .rotate(transform.createRotation())
            .scale(transform.scale);
    }
}
