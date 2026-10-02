package Glaxium.POV.actions.particle;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Offsets relative to a BBS actor: X right, Y up, Z forward in the actor's yaw. */
public final class ParticleSpaces
{
    private ParticleSpaces()
    {
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
}
