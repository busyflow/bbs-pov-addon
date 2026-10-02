package Glaxium.POV.hand.render;

import Glaxium.POV.hand.PovItemPose;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.RigBone;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/** Matrices of the actual first-person hand render, in camera/view space. */
public final class PovHandMatrices
{
    private static final Map<String, Entry> BONES = new HashMap<>();
    private static final Map<String, ItemEntry> ITEMS = new HashMap<>();
    private static final Map<String, Matrix4f> BODY_PART_BASES = new HashMap<>();
    /** Kept separately from BONES: bodyparts may use the same limb names, but
     * must never become the pose reused by the hand stencil/hover pass. */
    private static Pose capturedHandPose;

    private PovHandMatrices()
    {
    }

    public static void clear()
    {
        BONES.clear();
        ITEMS.clear();
        BODY_PART_BASES.clear();
        capturedHandPose = null;
    }

    /** Captures the vanilla item anchor and the final keyframed item matrix. */
    public static void captureItem(String bone, Matrix4f origin, Matrix4f full)
    {
        if (bone != null && origin != null && full != null)
        {
            ITEMS.put(bone, new ItemEntry(new Matrix4f(full), new Matrix4f(origin)));
        }
    }

    public static void capture(
        ModelInstance instance,
        ArmorSlot slot,
        Matrix4f renderBase,
        MatrixCache matrices,
        Pose pose)
    {
        if (instance == null || slot == null || slot.group == null
            || renderBase == null || matrices == null)
        {
            return;
        }

        IModel model = instance.getModel();

        if (model == null)
        {
            return;
        }

        Pose renderedPose = pose == null ? new Pose() : pose.copy();
        capturedHandPose = renderedPose.copy();

        for (String groupKey : model.getGroupKeysInHierarchyOrder())
        {
            RigBone bone = model.getBone(groupKey);

            if (!belongsTo(bone, slot.group))
            {
                continue;
            }

            MatrixCacheEntry entry = matrices.get(groupKey);

            if (entry == null || entry.matrix() == null || entry.origin() == null)
            {
                continue;
            }

            BONES.put(groupKey, new Entry(
                new Matrix4f(renderBase).mul(entry.matrix()),
                new Matrix4f(renderBase).mul(entry.origin()),
                instance,
                new Matrix4f(renderBase),
                renderedPose.copy(),
                entry.evaluatedRotation() == null ? null : new Vector3f(entry.evaluatedRotation())));
        }
    }

    /** Capture every bone of a POV bodypart in the same camera-space used by
     * the visible render. Keys use BBS's nested form path (e.g. "0/bone", "0/0/bone"),
     * which is also what UIKeyframeEditor returns for per-limb bodypart Pose tracks. */
    public static void captureBodyPart(
        String path,
        ModelFormRenderer renderer,
        IEntity entity,
        MatrixStack parentAttachment,
        Transform partTransform)
    {
        ModelInstance instance = renderer == null ? null : renderer.getModel();

        if (instance == null || instance.getModel() == null || parentAttachment == null || path == null || path.isBlank())
        {
            return;
        }

        Matrix4f baseMatrix = new Matrix4f(parentAttachment.peek().getPositionMatrix());
        BODY_PART_BASES.put(path, baseMatrix);

        MatrixStack renderStack = new MatrixStack();
        renderStack.peek().getPositionMatrix().set(baseMatrix);
        if (partTransform != null)
        {
            MatrixStackUtils.applyTransform(renderStack, partTransform);
        }

        MatrixCache rendered = new MatrixCache();
        renderer.collectMatrices(entity, renderStack, rendered, "", 0F);
        MatrixCacheEntry root = rendered.get("");
        if (root != null && root.matrix() != null)
        {
            ItemEntry itemEntry = new ItemEntry(new Matrix4f(root.matrix()), baseMatrix);
            ITEMS.put(path, itemEntry);
        }
        MatrixCache local = new MatrixCache();
        instance.captureMatrices(local);
        Pose pose = renderer.getPose();
        Pose capturedPose = pose == null ? new Pose() : pose.copy();

        /* captureMatrices includes cubic groups AND BOBJ armature bones.
         * BOBJModel.getAllGroups() is empty, despite having a full Pose tree. */
        for (String bone : local.keySet())
        {
            MatrixCacheEntry worldEntry = rendered.get(bone);
            MatrixCacheEntry localEntry = local.get(bone);

            if (worldEntry.matrix() == null || worldEntry.origin() == null
                || localEntry.matrix() == null)
            {
                continue;
            }

            Matrix4f renderBase = new Matrix4f(worldEntry.matrix())
                .mul(new Matrix4f(localEntry.matrix()).invert());
            Entry entry = new Entry(
                new Matrix4f(worldEntry.matrix()),
                new Matrix4f(worldEntry.origin()),
                instance,
                renderBase,
                capturedPose.copy(),
                worldEntry.evaluatedRotation() == null ? null : new Vector3f(worldEntry.evaluatedRotation()));

            BONES.put(path + "/" + bone, entry);
        }
    }

    private static boolean belongsTo(RigBone bone, String root)
    {
        for (RigBone current = bone; current != null; current = current.getParentBone())
        {
            if (root.equals(current.getBoneName()))
            {
                return true;
            }
        }

        return false;
    }

    /** Full matrix is used for translating/rotating the bone's local values. */
    public static Matrix4f getFull(String bone)
    {
        Entry entry = BONES.get(bone);
        ItemEntry item = ITEMS.get(bone);

        return entry != null ? new Matrix4f(entry.full)
            : item == null ? null : new Matrix4f(item.full);
    }

    /** Origin excludes the selected bone transform and represents global mode. */
    public static Matrix4f getOrigin(String bone)
    {
        Entry entry = BONES.get(bone);
        ItemEntry item = ITEMS.get(bone);

        return entry != null ? new Matrix4f(entry.origin)
            : item == null ? null : new Matrix4f(item.origin);
    }

    /** Pose used by the visible hand pass. The stencil pass must reuse this
     * exact result so its hover outline cannot trail a live gizmo edit. */
    public static Pose getCapturedPose()
    {
        return capturedHandPose == null ? null : capturedHandPose.copy();
    }

    /** Matrix/basis displayed by the gizmo for the requested BBS transform
     * space. All matrices remain in first-person camera space. */
    public static Matrix4f getForSpace(String bone, TransformSpace space)
    {
        Entry entry = BONES.get(bone);

        if (entry == null)
        {
            return getItemForSpace(bone, space);
        }

        if (space == TransformSpace.PARENT)
        {
            return new Matrix4f(entry.origin);
        }

        Matrix4f result = new Matrix4f(entry.full);

        if (space == TransformSpace.GLOBAL)
        {
            replaceBasis(result, entry.renderBase);
        }
        else if (space == TransformSpace.VIEW || space == TransformSpace.WORLD)
        {
            Vector3f position = result.getTranslation(new Vector3f());
            result.identity().setTranslation(position);
        }

        return MatrixStackUtils.stripScale(result);
    }

    private static Matrix4f getItemForSpace(String bone, TransformSpace space)
    {
        ItemEntry item = ITEMS.get(bone);

        if (item == null)
        {
            return null;
        }

        Matrix4f result = space == TransformSpace.PARENT
            ? (BODY_PART_BASES.containsKey(bone) ? new Matrix4f(BODY_PART_BASES.get(bone)) : new Matrix4f(item.origin))
            : new Matrix4f(item.full);

        if (space == TransformSpace.GLOBAL)
        {
            Matrix4f base = BODY_PART_BASES.get(bone);
            if (base != null)
            {
                replaceBasis(result, base);
            }
            else
            {
                Vector3f position = result.getTranslation(new Vector3f());
                result.identity().setTranslation(position);
            }
        }
        else if (space == TransformSpace.VIEW
            || space == TransformSpace.WORLD)
        {
            Vector3f position = result.getTranslation(new Vector3f());
            result.identity().setTranslation(position);
        }

        return MatrixStackUtils.stripScale(result);
    }

    public static Matrix3f getBasisForSpace(String bone, TransformSpace space)
    {
        Matrix4f matrix = getForSpace(bone, space);

        return matrix == null ? null : basis(matrix);
    }

    public static Matrix3f getGlobalBasis(String bone)
    {
        Entry entry = BONES.get(bone);

        if (entry == null && ITEMS.containsKey(bone))
        {
            Matrix4f base = BODY_PART_BASES.get(bone);
            return base != null ? basis(base) : new Matrix3f();
        }

        return entry == null
            ? null
            : basis(entry.renderBase);
    }

    /**
     * Euler rotation already present underneath the currently edited POV Pose
     * track. BBS pose tracks are additive, so the drag math must compose at the
     * rendered rotation and subtract this base again before writing the fields.
     * Without it the first drag decomposes around the track's often-zero Euler
     * values, which loses Z and can select successively wound 360-degree branches.
     */
    public static Vector3f getAdditiveRotationBase(String bone, Transform editedTrack)
    {
        Entry entry = BONES.get(bone);

        if (entry == null || entry.evaluatedRotation == null
            || !(editedTrack instanceof PoseTransform poseTrack)
            || poseTrack.rotationMode == Transform.RotationMode.QUATERNION
            || poseTrack.fix != 0F)
        {
            return null;
        }

        return new Vector3f(entry.evaluatedRotation).sub(poseTrack.rotate);
    }

    private static void replaceBasis(Matrix4f target, Matrix4f source)
    {
        Vector3f position = target.getTranslation(new Vector3f());
        Matrix3f rotation = basis(source);

        target.set(rotation).setTranslation(position);
    }

    private static Matrix3f basis(Matrix4f matrix)
    {
        return MatrixStackUtils.stripScale(new Matrix4f(matrix)).get3x3(new Matrix3f());
    }

    /** Rebuild the selected bone exactly as BBS renders it. This intentionally
     * uses ModelInstance.captureMatrices() instead of Transform.createMatrix():
     * cubic pose translation is mapped as (-x, y, z) / 16 and occurs around the
     * model group's pivot. */
    public static synchronized Matrix4f evaluateFull(
        String bone,
        Transform baseline,
        Transform edited)
    {
        Entry context = BONES.get(bone);
        ItemEntry item = ITEMS.get(bone);

        Matrix4f attachment = BODY_PART_BASES.get(bone);
        if (attachment != null && edited != null && context == null)
        {
            MatrixStack stack = new MatrixStack();
            MatrixStackUtils.multiply(stack, attachment);
            MatrixStackUtils.applyTransform(stack, edited);
            return MatrixStackUtils.stripScale(new Matrix4f(stack.peek().getPositionMatrix()));
        }

        if (context == null && item != null && edited != null)
        {
            return MatrixStackUtils.stripScale(
                new Matrix4f(item.origin).mul(PovItemPose.createMatrix(edited)));
        }
        ModelInstance instance = context == null ? null : context.instance;
        Matrix4f renderBase = context == null ? null : context.renderBase;
        Pose pose = context == null ? null : context.pose;

        if (bone == null || baseline == null || edited == null
            || instance == null || instance.getModel() == null
            || renderBase == null || pose == null)
        {
            return getFull(bone);
        }

        IModel model = instance.getModel();
        String modelBone = bone.contains("/") ? bone.substring(bone.lastIndexOf('/') + 1) : bone;
        Pose working = pose.copy();
        PoseTransform transform = working.transforms.computeIfAbsent(
            modelBone,
            ignored -> new PoseTransform());

        transform.translate.add(
            edited.translate.x - baseline.translate.x,
            edited.translate.y - baseline.translate.y,
            edited.translate.z - baseline.translate.z);
        Transform rotationDelta = new Transform();
        rotationDelta.setModeQuaternion();
        rotationDelta.quat.set(baseline.createRotation()).invert().mul(edited.createRotation());
        transform.addRotation(rotationDelta);
        transform.scale.mul(
            ratio(edited.scale.x, baseline.scale.x),
            ratio(edited.scale.y, baseline.scale.y),
            ratio(edited.scale.z, baseline.scale.z));

        try
        {
            model.resetPose();
            model.applyPose(working);

            MatrixCache matrices = new MatrixCache();
            instance.captureMatrices(matrices);
            MatrixCacheEntry entry = matrices.get(modelBone);

            if (entry == null || entry.matrix() == null)
            {
                return getFull(bone);
            }

            return MatrixStackUtils.stripScale(
                new Matrix4f(renderBase).mul(entry.matrix()));
        }
        finally
        {
            model.resetPose();
            model.applyPose(pose);
        }
    }

    private static float ratio(float value, float baseline)
    {
        return Math.abs(baseline) < 0.000001F ? 1F : value / baseline;
    }

    private record Entry(
        Matrix4f full,
        Matrix4f origin,
        ModelInstance instance,
        Matrix4f renderBase,
        Pose pose,
        Vector3f evaluatedRotation)
    {
    }

    private record ItemEntry(Matrix4f full, Matrix4f origin)
    {
    }
}
