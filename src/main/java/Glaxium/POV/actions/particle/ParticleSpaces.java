package Glaxium.POV.actions.particle;

import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

/** Offsets relative to a BBS actor: X right, Y up, Z forward in the actor's yaw. */
public final class ParticleSpaces
{
    public static final class ActorAnchor
    {
        public final Matrix4f matrix;
        public final boolean hipCentered;

        public ActorAnchor(Matrix4f matrix, boolean hipCentered)
        {
            this.matrix = matrix;
            this.hipCentered = hipCentered;
        }
    }

    private ParticleSpaces()
    {
    }

    public static Matrix4f resolveActorMatrix(
        Map<String, IEntity> entities,
        IEntity entity,
        Replay replay,
        float replayTick)
    {
        ActorAnchor anchor = resolveActorAnchor(entities, entity, replay, replayTick);

        return anchor != null ? anchor.matrix : null;
    }

    public static ActorAnchor resolveActorAnchor(
        Map<String, IEntity> entities,
        IEntity entity,
        Replay replay,
        float replayTick)
    {
        Matrix4f baseMatrix = resolveActorBaseMatrix(entities, entity, replay, replayTick);

        if (baseMatrix == null)
        {
            return null;
        }

        Form form = entity.getForm();

        if (form != null)
        {
            Form root = FormUtils.getRoot(form);
            FormRenderer renderer = root != null ? FormUtilsClient.getRenderer(root) : null;

            if (renderer != null)
            {
                MatrixCache cache = renderer.collectMatrices(entity, 1F);

                if (cache != null)
                {
                    Matrix4f boneMatrix = findBoneMatrix(cache, "low_body");

                    if (boneMatrix == null)
                    {
                        boneMatrix = findBoneMatrix(cache, "lower_body");
                    }

                    if (boneMatrix != null && boneMatrix.isFinite())
                    {
                        Matrix4f combined = new Matrix4f(baseMatrix).mul(boneMatrix);

                        if (combined.isFinite())
                        {
                            return new ActorAnchor(combined, true);
                        }
                    }

                    Matrix4f formMatrix = cache.has("") ? cache.get("").matrix() : null;

                    if (formMatrix != null && formMatrix.isFinite())
                    {
                        Matrix4f combined = new Matrix4f(baseMatrix).mul(formMatrix);

                        if (combined.isFinite())
                        {
                            return new ActorAnchor(combined, false);
                        }
                    }
                }
            }
        }

        return new ActorAnchor(baseMatrix, false);
    }

    private static Matrix4f resolveActorBaseMatrix(
        Map<String, IEntity> entities,
        IEntity entity,
        Replay replay,
        float replayTick)
    {
        if (entity == null)
        {
            return null;
        }

        if (replay != null && replay.relative.get())
        {
            Matrix4f relativeMatrix = FilmMatrices.getMatrixForRenderWithRotation(entity, 0D, 0D, 0D, 1F);

            return relativeMatrix != null && relativeMatrix.isFinite() ? relativeMatrix : null;
        }

        Anchor anchor = replay != null && replay.keyframes != null && !replay.keyframes.anchor.isEmpty()
            ? replay.evaluateAnchor(replayTick)
            : FilmMatrices.getAnchor(entity, replay);
        Matrix4f matrix = entities == null
            ? null
            : FilmMatrices.getAnchorMatrix(entities, entity, replay, 0D, 0D, 0D, 1F, anchor);

        if (matrix == null || !matrix.isFinite())
        {
            matrix = FilmMatrices.getMatrixForRenderWithRotation(entity, 0D, 0D, 0D, 1F);
        }

        return matrix != null && matrix.isFinite() ? matrix : null;
    }

    private static Matrix4f findBoneMatrix(MatrixCache cache, String boneName)
    {
        if (cache.has(boneName))
        {
            Matrix4f direct = cache.get(boneName).matrix();

            if (direct != null && direct.isFinite())
            {
                return direct;
            }
        }

        String suffix = "/" + boneName;

        for (String key : cache.keySet())
        {
            if (key != null && key.endsWith(suffix))
            {
                Matrix4f candidate = cache.get(key).matrix();

                if (candidate != null && candidate.isFinite())
                {
                    return candidate;
                }
            }
        }

        return null;
    }

    public static Vec3d matrixOrigin(Matrix4f matrix)
    {
        Vector3f pos = matrix.getTranslation(new Vector3f());

        return new Vec3d(pos.x, pos.y, pos.z);
    }

    public static Vec3d lerpPos(IEntity entity, float tickDelta)
    {
        return new Vec3d(
            Lerps.lerp(entity.getPrevX(), entity.getX(), tickDelta),
            Lerps.lerp(entity.getPrevY(), entity.getY(), tickDelta),
            Lerps.lerp(entity.getPrevZ(), entity.getZ(), tickDelta));
    }

    public static float lerpYaw(IEntity entity, float tickDelta)
    {
        return (float) Lerps.lerpYaw(entity.getPrevHeadYaw(), entity.getHeadYaw(), tickDelta);
    }

    public static float width(IEntity entity)
    {
        if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living)
        {
            return living.getWidth();
        }

        return 0.6F;
    }

    public static float height(IEntity entity)
    {
        if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living)
        {
            return living.getHeight();
        }

        return 1.8F;
    }

    public static Vec3d entityToWorld(IEntity entity, double x, double y, double z, float tickDelta)
    {
        float yaw = lerpYaw(entity, tickDelta) * MathHelper.RADIANS_PER_DEGREE;

        return lerpPos(entity, tickDelta).add(new Vec3d(x, y, z).rotateY(-yaw));
    }

    public static Vec3d worldToEntity(double originX, double originY, double originZ, float yawDegrees, Vec3d world)
    {
        return world.subtract(originX, originY, originZ)
            .rotateY(yawDegrees * MathHelper.RADIANS_PER_DEGREE);
    }

    public static Vec3d pointInActorAabb(Vec3d origin, float width, float height, java.util.Random random)
    {
        return origin.add(
            (random.nextDouble() - 0.5D) * width,
            random.nextDouble() * height,
            (random.nextDouble() - 0.5D) * width);
    }

    public static Vec3d pointInActorBox(
        Matrix4f actorMatrix,
        Vec3d fallbackOrigin,
        float width,
        float height,
        java.util.Random random)
    {
        return pointInActorBox(actorMatrix, false, fallbackOrigin, width, height, random);
    }

    public static Vec3d pointInActorBox(
        ActorAnchor anchor,
        Vec3d fallbackOrigin,
        float width,
        float height,
        java.util.Random random)
    {
        return pointInActorBox(
            anchor != null ? anchor.matrix : null,
            anchor != null && anchor.hipCentered,
            fallbackOrigin,
            width,
            height,
            random);
    }

    private static Vec3d pointInActorBox(
        Matrix4f actorMatrix,
        boolean hipCentered,
        Vec3d fallbackOrigin,
        float width,
        float height,
        java.util.Random random)
    {
        double lx = (random.nextDouble() - 0.5D) * width;
        double minY = hipCentered ? -0.42D * (double) height : 0.0D;
        double ly = minY + random.nextDouble() * height;
        double lz = (random.nextDouble() - 0.5D) * width;

        return transformLocalPoint(actorMatrix, fallbackOrigin, lx, ly, lz);
    }

    public static Vec3d pointAtActorFeet(
        Matrix4f actorMatrix,
        Vec3d fallbackOrigin,
        float width,
        java.util.Random random)
    {
        double lx = (random.nextDouble() - 0.5D) * width;
        double lz = (random.nextDouble() - 0.5D) * width;

        return transformLocalPoint(actorMatrix, fallbackOrigin, lx, 0.1D, lz);
    }

    public static Vec3d pointAtActorFeet(
        ActorAnchor anchor,
        Vec3d fallbackOrigin,
        float width,
        float height,
        java.util.Random random)
    {
        boolean hipCentered = anchor != null && anchor.hipCentered;
        double lx = (random.nextDouble() - 0.5D) * width;
        double ly = hipCentered ? -0.40D * (double) height : 0.1D;
        double lz = (random.nextDouble() - 0.5D) * width;

        return transformLocalPoint(anchor != null ? anchor.matrix : null, fallbackOrigin, lx, ly, lz);
    }

    public static Vec3d pointAtActorHeadBase(Matrix4f actorMatrix, Vec3d fallbackOrigin, float height)
    {
        return transformLocalPoint(actorMatrix, fallbackOrigin, 0.0D, (double) height * 0.85D, 0.0D);
    }

    public static Vec3d pointAtActorHeadBase(ActorAnchor anchor, Vec3d fallbackOrigin, float height)
    {
        boolean hipCentered = anchor != null && anchor.hipCentered;
        double ly = (double) height * (hipCentered ? 0.43D : 0.85D);

        return transformLocalPoint(anchor != null ? anchor.matrix : null, fallbackOrigin, 0.0D, ly, 0.0D);
    }

    public static Vec3d pointInTotemBurst(
        ActorAnchor anchor,
        Vec3d fallbackOrigin,
        float width,
        float height,
        double d,
        double e,
        double f)
    {
        boolean hipCentered = anchor != null && anchor.hipCentered;
        double lx = (d / 4.0D) * (double) width;
        double ly = ((hipCentered ? 0.08D : 0.5D) + e / 4.0D) * (double) height;
        double lz = (f / 4.0D) * (double) width;

        return transformLocalPoint(anchor != null ? anchor.matrix : null, fallbackOrigin, lx, ly, lz);
    }

    public static float resolveActorYaw(Matrix4f actorMatrix, IEntity entity, Replay replay, float replayTick)
    {
        if (actorMatrix != null && actorMatrix.isFinite())
        {
            Vector3f forward = actorMatrix.transformDirection(0F, 0F, 1F, new Vector3f());

            if (forward.lengthSquared() > 1.0E-6F && Float.isFinite(forward.x) && Float.isFinite(forward.z))
            {
                float matrixYaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));

                if (entity != null)
                {
                    return matrixYaw + (entity.getHeadYaw() - entity.getBodyYaw());
                }

                return matrixYaw;
            }
        }

        if (entity != null)
        {
            return entity.getHeadYaw();
        }

        if (replay != null && replay.keyframes != null)
        {
            return (float) replay.keyframes.yaw.interpolate(replayTick, 0.0).doubleValue();
        }

        return 0F;
    }

    private static Vec3d transformLocalPoint(
        Matrix4f actorMatrix,
        Vec3d fallbackOrigin,
        double lx,
        double ly,
        double lz)
    {
        if (actorMatrix != null && actorMatrix.isFinite())
        {
            Vector3f point = actorMatrix.transformPosition((float) lx, (float) ly, (float) lz, new Vector3f());

            if (Float.isFinite(point.x) && Float.isFinite(point.y) && Float.isFinite(point.z))
            {
                return new Vec3d(point.x, point.y, point.z);
            }
        }

        return fallbackOrigin.add(lx, ly, lz);
    }
}
