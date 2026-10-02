package Glaxium.POV.hud.render;

import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.hud.HudState;
import Glaxium.POV.integration.access.minecraft.InGameHudHeldItemPovAccess;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

/**
 * Vanilla selected-hotbar item name: appears on slot/item change and fades out.
 * Derived from recorded hotbar + selected slot — no extra keyframes.
 */
public final class HeldItemTooltipRenderer
{
    private HeldItemTooltipRenderer()
    {
    }

    public static void renderPlayback(
        Batcher2D batcher,
        ReplayKeyframes replay,
        HudState state,
        float tick,
        int width,
        int height)
    {
        if (state == null || replay == null || !state.visible)
        {
            return;
        }

        int slot = MathHelper.clamp(state.selectedSlot, 0, ReplayKeyframes.HOTBAR_SIZE - 1);
        ItemStack stack = state.items[slot];
        int fade = remainingFade(replay, tick, stack);
        render(batcher, stack, fade, state.statusBarsVisible, state.layout, width, height);
    }

    public static void renderLive(Batcher2D batcher, HudState state, int width, int height)
    {
        if (state == null || !state.visible)
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        InGameHud hud = client.inGameHud;
        if (!(hud instanceof InGameHudHeldItemPovAccess access))
        {
            return;
        }

        ItemStack stack = access.bbsPov$getHeldItemTooltipStack();
        if (stack == null || stack.isEmpty())
        {
            int slot = MathHelper.clamp(state.selectedSlot, 0, 8);
            stack = state.items[slot];
        }

        render(
            batcher,
            stack,
            access.bbsPov$getHeldItemTooltipFade(),
            state.statusBarsVisible,
            state.layout,
            width,
            height);
    }

    public static void render(
        Batcher2D batcher,
        ItemStack stack,
        int fade,
        boolean statusBarsVisible,
        Transform layout,
        int width,
        int height)
    {
        if (fade <= 0 || stack == null || stack.isEmpty())
        {
            return;
        }

        MutableText name = Text.empty().append(stack.getName()).formatted(stack.getRarity().formatting);
        if (stack.hasCustomName())
        {
            name = name.formatted(Formatting.ITALIC);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer texts = client.textRenderer;
        int textWidth = texts.getWidth(name);
        float layoutX = layout == null ? 0F : (float) layout.translate.x * HotbarLayoutTransform.PIXELS_PER_UNIT;
        float layoutY = layout == null ? 0F : (float) layout.translate.y * HotbarLayoutTransform.PIXELS_PER_UNIT;
        int x = (width - textWidth) / 2 + Math.round(layoutX);
        int y = height - 59 - Math.round(layoutY);
        if (!statusBarsVisible)
        {
            y += 14;
        }

        int alpha = (int) ((float) fade * 256.0F / 10.0F);
        if (alpha > 255)
        {
            alpha = 255;
        }
        if (alpha <= 0)
        {
            return;
        }

        DrawContext context = batcher.getContext();
        batcher.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int background = client.options.getTextBackgroundColor(0);
        context.fill(x - 2, y - 2, x + textWidth + 2, y + 9 + 2, background);
        context.drawTextWithShadow(texts, name, x, y, 16777215 + (alpha << 24));
        context.draw();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        batcher.flush();
    }

    private static int remainingFade(ReplayKeyframes replay, float tick, ItemStack now)
    {
        if (now == null || now.isEmpty())
        {
            return 0;
        }

        double displayTime = 1.0D;
        var option = MinecraftClient.getInstance().options.getNotificationDisplayTime();
        if (option != null && option.getValue() != null)
        {
            displayTime = option.getValue();
        }

        int max = Math.max(1, (int) (40.0D * displayTime));
        for (int i = 1; i <= max + 1; i++)
        {
            float thenTick = tick - i;
            /* Before the recording starts the actor was already holding this
             * stack — vanilla would have finished fading, so do not pop the name. */
            if (thenTick < 0F)
            {
                return 0;
            }

            int thenSlot = MathHelper.clamp(replay.getSelectedSlot(thenTick), 0, ReplayKeyframes.HOTBAR_SIZE - 1);
            ItemStack then = replay.hotbar.get(thenSlot).interpolate(thenTick, ItemStack.EMPTY);
            if (!sameHeldName(then, now))
            {
                return Math.max(0, max - i + 1);
            }
        }

        return 0;
    }

    private static boolean sameHeldName(ItemStack a, ItemStack b)
    {
        boolean aEmpty = a == null || a.isEmpty();
        boolean bEmpty = b == null || b.isEmpty();
        if (aEmpty || bEmpty)
        {
            return aEmpty && bEmpty;
        }

        return a.isOf(b.getItem()) && a.getName().equals(b.getName());
    }
}
