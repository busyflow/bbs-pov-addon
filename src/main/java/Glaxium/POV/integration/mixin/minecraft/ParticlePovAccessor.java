package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ParticlePovAccess;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticlePovAccessor extends ParticlePovAccess
{
    @Accessor("x")
    double bbsPov$getX();

    @Accessor("y")
    double bbsPov$getY();

    @Accessor("z")
    double bbsPov$getZ();

    @Accessor("x")
    void bbsPov$setX(double x);

    @Accessor("y")
    void bbsPov$setY(double y);

    @Accessor("z")
    void bbsPov$setZ(double z);

    @Accessor("prevPosX")
    void bbsPov$setPrevPosX(double x);

    @Accessor("prevPosY")
    void bbsPov$setPrevPosY(double y);

    @Accessor("prevPosZ")
    void bbsPov$setPrevPosZ(double z);

    @Accessor("gravityStrength")
    float bbsPov$getGravityStrength();

    @Accessor("velocityMultiplier")
    float bbsPov$getVelocityMultiplier();

    @Accessor("maxAge")
    int bbsPov$getMaxAge();

    @Accessor("age")
    void bbsPov$setAge(int age);
}
