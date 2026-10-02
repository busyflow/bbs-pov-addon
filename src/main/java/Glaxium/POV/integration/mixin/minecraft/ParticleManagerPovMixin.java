package Glaxium.POV.integration.mixin.minecraft;

import Glaxium.POV.actions.particle.ParticleActionRenderer;
import Glaxium.POV.actions.particle.recording.ParticleRecorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerPovMixin
{
    private static final double LIVE_PLAYER_RANGE = 4.5;

    @Inject(method = "tick", at = @At("HEAD"))
    private void bbsPov$tickFilmParticles(CallbackInfo ci)
    {
        ParticleActionRenderer.tick();
    }

    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true)
    private void bbsPov$captureParticle(
        ParticleEffect parameters,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz,
        CallbackInfoReturnable<Particle> cir)
    {
        if (hideLivePlayerParticle(x, y, z))
        {
            cir.setReturnValue(null);
            return;
        }

        ParticleRecorder.capture(parameters, x, y, z);
    }

    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V", at = @At("HEAD"), cancellable = true)
    private void bbsPov$captureEmitter(Entity entity, ParticleEffect parameters, CallbackInfo ci)
    {
        if (hideLivePlayerEmitter(entity))
        {
            ci.cancel();
            return;
        }

        if (entity != null && entity == MinecraftClient.getInstance().player)
        {
            ParticleRecorder.capture(parameters, entity.getX(), entity.getY(), entity.getZ());
        }
    }

    @Inject(
        method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V",
        at = @At("HEAD"),
        cancellable = true)
    private void bbsPov$captureTimedEmitter(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci)
    {
        if (hideLivePlayerEmitter(entity))
        {
            ci.cancel();
        }
    }

    private static boolean hideLivePlayerParticle(double x, double y, double z)
    {
        if (!ParticleActionRenderer.hidesLivePlayerParticles())
        {
            return false;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        return player != null && player.squaredDistanceTo(x, y, z) < LIVE_PLAYER_RANGE * LIVE_PLAYER_RANGE;
    }

    private static boolean hideLivePlayerEmitter(Entity entity)
    {
        return ParticleActionRenderer.hidesLivePlayerParticles()
            && entity != null
            && entity == MinecraftClient.getInstance().player;
    }
}
