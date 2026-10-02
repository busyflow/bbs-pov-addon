package Glaxium.POV.integration.access.minecraft;

public interface ParticlePovAccess
{
    double bbsPov$getX();

    double bbsPov$getY();

    double bbsPov$getZ();

    void bbsPov$setX(double x);

    void bbsPov$setY(double y);

    void bbsPov$setZ(double z);

    void bbsPov$setPrevPosX(double x);

    void bbsPov$setPrevPosY(double y);

    void bbsPov$setPrevPosZ(double z);

    float bbsPov$getGravityStrength();

    float bbsPov$getVelocityMultiplier();

    int bbsPov$getMaxAge();

    void bbsPov$setAge(int age);
}
