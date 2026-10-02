package wemppy.bbs_pov.client.render;

import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.overwrite.POVClip;
import mchorse.bbs_mod.camera.clips.overwrite.PovHudData;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.Random;

public class PovParticleEmitter
{
    private static final Random RANDOM = new Random();
    private static int lastChewTick = -1;

    public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context)
    {
        List<PovHudData> huds = POVClip.getPovHuds(context);

        if (huds == null || huds.isEmpty())
        {
            return;
        }

        PovHudData hudData = huds.get(huds.size() - 1);
        if (hudData == null || hudData.factor <= 0F)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        IEntity entity = null;
        if (context instanceof CameraClipContext cameraContext)
        {
            entity = cameraContext.entities.get(hudData.replay.getId());
        }

        if (entity instanceof MCEntity mcEnt && mcEnt.getMcEntity() instanceof LivingEntity living)
        {
            if (living.isUsingItem())
            {
                ItemStack usingItem = living.getActiveItem();
                int useTime = living.getItemUseTime();

                // Spawn chewing / drinking crumbs every 4 ticks
                if (!usingItem.isEmpty() && useTime % 4 == 0 && lastChewTick != context.ticks)
                {
                    lastChewTick = context.ticks;
                    boolean isDrink = usingItem.getItem() instanceof PotionItem;

                    // Emit particles right in front of camera
                    double cx = mc.player.getX();
                    double cy = mc.player.getEyeY() - 0.2D;
                    double cz = mc.player.getZ();

                    for (int i = 0; i < 3; i++)
                    {
                        double px = cx + (RANDOM.nextDouble() - 0.5D) * 0.2D;
                        double py = cy + (RANDOM.nextDouble() - 0.5D) * 0.2D;
                        double pz = cz + (RANDOM.nextDouble() - 0.5D) * 0.2D;

                        double vx = (RANDOM.nextDouble() - 0.5D) * 0.1D;
                        double vy = RANDOM.nextDouble() * 0.1D;
                        double vz = (RANDOM.nextDouble() - 0.5D) * 0.1D;

                        mc.world.addParticle(new ItemStackParticleEffect(ParticleTypes.ITEM, usingItem), px, py, pz, vx, vy, vz);
                    }

                    // Play chewing/drinking sound
                    if (isDrink)
                    {
                        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_GENERIC_DRINK, 0.5F));
                    }
                    else
                    {
                        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_GENERIC_EAT, 0.5F + 0.4F * RANDOM.nextFloat()));
                    }
                }
            }
        }
    }
}
