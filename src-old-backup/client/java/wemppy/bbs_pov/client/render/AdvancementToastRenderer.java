package wemppy.bbs_pov.client.render;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import wemppy.bbs_pov.clips.ToastData;

import java.util.ArrayList;
import java.util.List;

public class AdvancementToastRenderer
{
    private static final Identifier TOASTS = new Identifier("minecraft", "textures/gui/toasts.png");

    public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context)
    {
        List<ToastData> list = context.clipData.get("bbs_advancement_toasts", ArrayList::new);

        if (list == null || list.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext drawContext = batcher.getContext();
        int width = mc.getWindow().getScaledWidth();

        for (ToastData data : list)
        {
            if (data.factor <= 0F)
            {
                continue;
            }

            // 1. Advancement toast in top-right
            int toastWidth = 160;
            int toastHeight = 32;
            int toastX = width - toastWidth - 8;

            // Slide in animation for first 5 ticks
            float slideProgress = MathHelper.clamp(data.relTick / 5.0F, 0.0F, 1.0F);
            int toastY = (int) (-toastHeight * (1.0F - slideProgress) + 4.0F * slideProgress);

            // Background toast texture
            drawContext.drawTexture(TOASTS, toastX, toastY, 0, 0, toastWidth, toastHeight);

            // Icon
            if (data.icon != null && !data.icon.isEmpty())
            {
                drawContext.drawItem(data.icon, toastX + 8, toastY + 8);
            }

            // Title & Description
            drawContext.drawText(mc.textRenderer, Text.literal(data.title), toastX + 30, toastY + 7, 0x884488, false);
            drawContext.drawText(mc.textRenderer, Text.literal(data.description), toastX + 30, toastY + 18, 0xFFFFFF, false);

            // 2. Top notification banner (e.g. "1 was saved!" with green border from Image 2)
            if (data.banner != null && !data.banner.trim().isEmpty())
            {
                String bannerMsg = data.banner;
                int textW = mc.textRenderer.getWidth(bannerMsg);
                int bannerW = Math.max(textW + 30, 200);
                int bannerH = 18;
                int bannerX = width / 2 - bannerW / 2;
                int bannerY = 8;

                // Dark fill + green border
                batcher.box(bannerX, bannerY, bannerX + bannerW, bannerY + bannerH, 0xCC111111);
                batcher.outline(bannerX, bannerY, bannerX + bannerW, bannerY + bannerH, 0xFF22AA44);

                drawContext.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(bannerMsg), width / 2, bannerY + 5, 0xFFFFFFFF);
            }
        }
    }
}
