package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ParticleManagerPovAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParticleManager.class)
public interface ParticleManagerPovAccessor extends ParticleManagerPovAccess
{
    @Invoker("createParticle")
    Particle bbsPov$createParticle(ParticleEffect parameters, double x, double y, double z, double vx, double vy, double vz);
}
