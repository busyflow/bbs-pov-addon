package Glaxium.POV.actions.bossbar.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.integration.access.minecraft.BossBarHudPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Draws a single vanilla boss bar at the top of the POV overlay. */
public final class BossBarActionRenderer
{
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int BAR_Y = 12;

    private BossBarActionRenderer()
    {
    }

    public static void render(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height)
    {
        if (actions == null)
        {
            return;
        }

        List<BossBarPovActionClip> activeClips = actions.getActiveBossBars(tick);
        if (activeClips.isEmpty())
        {
            return;
        }

        int baseLayer = Glaxium.POV.actions.PovActionType.BOSS_BARS.seedLayer();
        int maxSlots = 9;

        for (int i = 0; i < Math.min(maxSlots, activeClips.size()); i++)
        {
            BossBarPovActionClip clip = activeClips.get(i);
            float local = clip.getLocalTick(tick);
            int slot = Math.max(0, Math.min(8, clip.layer.get() - baseLayer));
            if (slot < 0 || slot >= maxSlots)
            {
                slot = i;
            }
            int y = BAR_Y + slot * 19;

            renderBar(
                batcher,
                clip.name.interpolate(local, "Ender Dragon"),
                clip.percent.interpolate(local, 1F),
                clip.color.interpolate(local, "pink"),
                clip.style.interpolate(local, "progress"),
                width,
                y);
        }
    }

    public static void renderLive(Batcher2D batcher, int width, int height)
    {
        MinecraftClient client = MinecraftClient.getInstance();
        BossBarHud hud = client.inGameHud == null ? null : client.inGameHud.getBossBarHud();
        if (!(hud instanceof BossBarHudPovAccess access))
        {
            return;
        }

        Map<UUID, ClientBossBar> bars = access.bbsPov$getBossBars();
        if (bars == null || bars.isEmpty())
        {
            return;
        }

        int slot = 0;
        for (ClientBossBar bar : bars.values())
        {
            if (bar != null)
            {
                int y = BAR_Y + slot * 19;
                renderBar(
                    batcher,
                    bar.getName().getString(),
                    bar.getPercent(),
                    BossBarLooks.colorId(bar.getColor()),
                    BossBarLooks.styleId(bar.getStyle()),
                    width,
                    y);
                slot++;
                if (slot >= 9) break;
            }
        }
    }

    private static void renderBar(
        Batcher2D batcher,
        String name,
        float percent,
        String color,
        String style,
        int screenWidth,
        int y)
    {
        int x = screenWidth / 2 - 91;
        int fill = MathHelper.lerpPositive(MathHelper.clamp(percent, 0F, 1F), 0, WIDTH);
        DrawContext context = batcher.getContext();

        batcher.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        /* Same two-pass clip as BossBarHud: empty track at full width, then
         * the bright fill clipped to health. The 5-arg drawGuiTexture nine-slices
         * these 5px sprites and leaves only the dark outline. */
        drawSprite(context, BossBarLooks.background(color), x, y, WIDTH);
        drawNotched(context, BossBarLooks.notchedBackground(style), x, y, WIDTH);
        if (fill > 0)
        {
            drawSprite(context, BossBarLooks.progress(color), x, y, fill);
            drawNotched(context, BossBarLooks.notchedProgress(style), x, y, fill);
        }

        TextRenderer texts = MinecraftClient.getInstance().textRenderer;
        Text title;
        if (name == null || name.isEmpty())
        {
            title = Text.empty();
        }
        else if ("Ender Dragon".equalsIgnoreCase(name))
        {
            title = Text.translatable("entity.minecraft.ender_dragon");
        }
        else if ("Wither".equalsIgnoreCase(name))
        {
            title = Text.translatable("entity.minecraft.wither");
        }
        else if ("Raid".equalsIgnoreCase(name))
        {
            title = Text.translatable("event.minecraft.raid");
        }
        else
        {
            title = Text.literal(name);
        }
        int textX = screenWidth / 2 - texts.getWidth(title) / 2;
        context.drawTextWithShadow(texts, title, textX, y - 9, 0xFFFFFF);
        context.draw();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        batcher.flush();
    }

    private static void drawNotched(DrawContext context, Identifier texture, int x, int y, int width)
    {
        if (texture == null)
        {
            return;
        }

        RenderSystem.enableBlend();
        drawSprite(context, texture, x, y, width);
        RenderSystem.disableBlend();
    }

    private static void drawSprite(DrawContext context, Identifier texture, int x, int y, int width)
    {
        context.drawGuiTexture(texture, WIDTH, HEIGHT, 0, 0, x, y, width, HEIGHT);
    }
}
