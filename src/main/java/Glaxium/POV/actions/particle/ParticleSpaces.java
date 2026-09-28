package Glaxium.POV.actions.particle;

import java.util.Random;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class ParticleSpaces {
   private ParticleSpaces() {
   }

   public static Vec3d lerpPos(IEntity entity, float tickDelta) {
      return new Vec3d(
         Lerps.lerp(entity.getPrevX(), entity.getX(), (double)tickDelta),
         Lerps.lerp(entity.getPrevY(), entity.getY(), (double)tickDelta),
         Lerps.lerp(entity.getPrevZ(), entity.getZ(), (double)tickDelta)
      );
   }

   public static float lerpYaw(IEntity entity, float tickDelta) {
      return (float)Lerps.lerpYaw((double)entity.getPrevHeadYaw(), (double)entity.getHeadYaw(), (double)tickDelta);
   }

   public static float width(IEntity entity) {
      if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) {
         return living.getWidth();
      }

      return 0.6F;
   }

   public static float height(IEntity entity) {
      if (entity instanceof MCEntity mc && mc.getMcEntity() instanceof LivingEntity living) {
         return living.getHeight();
      }

      return 1.8F;
   }

   public static Vec3d entityToWorld(IEntity entity, double x, double y, double z, float tickDelta) {
      float yaw = lerpYaw(entity, tickDelta) * (float) (Math.PI / 180.0);
      return lerpPos(entity, tickDelta).add(new Vec3d(x, y, z).rotateY(-yaw));
   }

   public static Vec3d worldToEntity(double originX, double originY, double originZ, float yawDegrees, Vec3d world) {
      return world.subtract(originX, originY, originZ).rotateY(yawDegrees * (float) (Math.PI / 180.0));
   }

   public static Vec3d pointInActorAabb(Vec3d origin, float width, float height, Random random) {
      return origin.add((random.nextDouble() - 0.5) * (double)width, random.nextDouble() * (double)height, (random.nextDouble() - 0.5) * (double)width);
   }
}
