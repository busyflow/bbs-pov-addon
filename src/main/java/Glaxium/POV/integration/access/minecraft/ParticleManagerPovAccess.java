package Glaxium.POV.integration.access.minecraft;

import net.minecraft.client.particle.Particle;
import net.minecraft.particle.ParticleEffect;

public interface ParticleManagerPovAccess
{
    Particle bbsPov$createParticle(ParticleEffect parameters, double x, double y, double z, double vx, double vy, double vz);
}
