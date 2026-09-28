package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.integration.access.minecraft.ParticleManagerPovAccess;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ParticleManager.class})
public interface ParticleManagerPovAccessor extends ParticleManagerPovAccess {
   @Invoker("createParticle")
   @Override
   Particle bbsPov$createParticle(ParticleEffect var1, double var2, double var4, double var6, double var8, double var10, double var12);
}
