package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ParticlePovAccess;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Particle.class})
public interface ParticlePovAccessor extends ParticlePovAccess {
   @Accessor("x")
   @Override
   double bbsPov$getX();

   @Accessor("y")
   @Override
   double bbsPov$getY();

   @Accessor("z")
   @Override
   double bbsPov$getZ();

   @Accessor("x")
   @Override
   void bbsPov$setX(double var1);

   @Accessor("y")
   @Override
   void bbsPov$setY(double var1);

   @Accessor("z")
   @Override
   void bbsPov$setZ(double var1);

   @Accessor("prevPosX")
   @Override
   void bbsPov$setPrevPosX(double var1);

   @Accessor("prevPosY")
   @Override
   void bbsPov$setPrevPosY(double var1);

   @Accessor("prevPosZ")
   @Override
   void bbsPov$setPrevPosZ(double var1);

   @Accessor("gravityStrength")
   @Override
   float bbsPov$getGravityStrength();

   @Accessor("velocityMultiplier")
   @Override
   float bbsPov$getVelocityMultiplier();

   @Accessor("maxAge")
   @Override
   int bbsPov$getMaxAge();

   @Accessor("age")
   @Override
   void bbsPov$setAge(int var1);
}
