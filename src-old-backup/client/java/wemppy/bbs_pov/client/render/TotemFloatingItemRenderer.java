package wemppy.bbs_pov.client.render;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import wemppy.bbs_pov.clips.ScreenEffectsData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TotemFloatingItemRenderer
{
    private static final Identifier POWDER_SNOW_OUTLINE = new Identifier("minecraft", "textures/misc/powder_snow_outline.png");
    private static final Random RANDOM = new Random();
    private static int lastSoundTick = -1;

    public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context)
    {
        List<ScreenEffectsData> list = context.clipData.get("bbs_screen_effects", ArrayList::new);

        if (list == null || list.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext drawContext = batcher.getContext();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        for (ScreenEffectsData data : list)
        {
            if (data.factor <= 0F)
            {
                continue;
            }

            String type = data.effectType.toLowerCase();

            if (type.equals("totem"))
            {
                renderTotemPop(drawContext, mc, width, height, data);
            }
            else if (type.equals("damage"))
            {
                renderDamageVignette(drawContext, width, height, data);
            }
            else if (type.equals("freeze"))
            {
                renderFreezeOverlay(drawContext, width, height, data);
            }
            else if (type.equals("portal"))
            {
                renderPortalOverlay(drawContext, width, height, data);
            }
        }
    }

    private static void renderTotemPop(DrawContext drawContext, MinecraftClient mc, int width, int height, ScreenEffectsData data)
    {
        float duration = Math.max(1.0F, data.duration);
        float progress = MathHelper.clamp(data.relTick / duration, 0.0F, 1.0F);

        // Vanilla floating item polynomial curve
        float f = progress;
        float g = f * f;
        float h = f * g;
        float j = 10.25F * h * g - 24.95F * g * g + 25.5F * h - 13.8F * g + 4.0F * f;
        float k = j * (float) Math.PI;
        float scale = (50.0F + 175.0F * MathHelper.sin(k)) * data.scale;

        // Sound on activation
        if (data.playSound && data.relTick >= 0 && data.relTick < 1.0F && lastSoundTick != data.absoluteTicks)
        {
            lastSoundTick = data.absoluteTicks;
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_TOTEM_USE, 1.0F));
        }

        // Particle burst
        if (data.showParticles && mc.world != null && progress < 0.75F && mc.player != null)
        {
            for (int p = 0; p < 3; p++)
            {
                double px = mc.player.getX() + (RANDOM.nextDouble() - 0.5D) * 1.5D;
                double py = mc.player.getY() + 0.5D + (RANDOM.nextDouble() - 0.5D) * 1.0D;
                double pz = mc.player.getZ() + (RANDOM.nextDouble() - 0.5D) * 1.5D;
                mc.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING, px, py, pz,
                    (RANDOM.nextDouble() - 0.5D) * 0.4D, RANDOM.nextDouble() * 0.4D, (RANDOM.nextDouble() - 0.5D) * 0.4D);
            }
        }

        ItemStack itemToRender = data.item != null && !data.item.isEmpty() ? data.item : new ItemStack(Items.TOTEM_OF_UNDYING);

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(width / 2.0F, height / 2.0F, 50.0F);

        float itemScale = scale / 16.0F;
        matrices.scale(itemScale, -itemScale, itemScale);

        // Wobble matching vanilla floating item
        float wobble = MathHelper.sin(k * 2.0F) * 6.0F;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wobble));
        matrices.translate(-8.0F, -8.0F, 0.0F);

        drawContext.drawItem(itemToRender, 0, 0);
        matrices.pop();
    }

    private static void renderDamageVignette(DrawContext drawContext, int width, int height, ScreenEffectsData data)
    {
        int alpha = (int) (data.factor * 120.0F);
        if (alpha > 0)
        {
            int color = (alpha << 24) | 0x900000;
            drawContext.fill(0, 0, width, height, color);
        }
    }

    private static void renderFreezeOverlay(DrawContext drawContext, int width, int height, ScreenEffectsData data)
    {
        int alpha = (int) (data.factor * 255.0F);
        if (alpha > 0)
        {
            drawContext.drawTexture(POWDER_SNOW_OUTLINE, 0, 0, 0.0F, 0.0F, width, height, width, height);
        }
    }

    private static void renderPortalOverlay(DrawContext drawContext, int width, int height, ScreenEffectsData data)
    {
        int alpha = (int) (data.factor * 140.0F);
        if (alpha > 0)
        {
            int color = (alpha << 24) | 0x4A0072;
            drawContext.fill(0, 0, width, height, color);
        }
    }
}
