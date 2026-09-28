package Glaxium.POV.integration.access.minecraft;

public interface ParticlePovAccess {
   double bbsPov$getX();

   double bbsPov$getY();

   double bbsPov$getZ();

   void bbsPov$setX(double var1);

   void bbsPov$setY(double var1);

   void bbsPov$setZ(double var1);

   void bbsPov$setPrevPosX(double var1);

   void bbsPov$setPrevPosY(double var1);

   void bbsPov$setPrevPosZ(double var1);

   float bbsPov$getGravityStrength();

   float bbsPov$getVelocityMultiplier();

   int bbsPov$getMaxAge();

   void bbsPov$setAge(int var1);
}
