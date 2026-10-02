package wemppy.bbs_pov.client.render;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import wemppy.bbs_pov.clips.MenuOverlayData;

import java.util.ArrayList;
import java.util.List;

public class DeathScreenOverlayRenderer
{
    private static final Identifier WIDGETS = new Identifier("minecraft", "textures/gui/widgets.png");

    public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context)
    {
        List<MenuOverlayData> list = context.clipData.get("bbs_menus", ArrayList::new);

        if (list == null || list.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        DrawContext drawContext = batcher.getContext();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        for (MenuOverlayData data : list)
        {
            if (data.factor <= 0F)
            {
                continue;
            }

            if ("death".equalsIgnoreCase(data.menuType))
            {
                renderDeathScreen(drawContext, mc, width, height, data);
            }
        }
    }

    private static void renderDeathScreen(DrawContext drawContext, MinecraftClient mc, int width, int height, MenuOverlayData data)
    {
        // Red background gradient
        int topColor = data.backgroundColor;
        int bottomColor = (data.backgroundColor & 0x00FFFFFF) | 0xA0000000;
        drawContext.fillGradient(0, 0, width, height, topColor, bottomColor);

        MatrixStack matrices = drawContext.getMatrices();

        // "You Died!" / "Game Over!" title (2.0x scale)
        matrices.push();
        matrices.scale(2.0F, 2.0F, 2.0F);
        String title = data.hardcore ? "Game Over!" : "You Died!";
        int titleX = (width / 2) / 2;
        int titleY = 30;
        drawContext.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(title), titleX, titleY, 0xFFFFFFFF);
        matrices.pop();

        // Death message
        if (data.deathMessage != null && !data.deathMessage.isEmpty())
        {
            drawContext.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(data.deathMessage), width / 2, 85, 0xFFFFFFFF);
        }

        // Score
        if (data.score != null && !data.score.isEmpty())
        {
            Text scoreText = Text.literal("Score: ").formatted(Formatting.WHITE)
                .append(Text.literal(data.score).formatted(Formatting.YELLOW));
            drawContext.drawCenteredTextWithShadow(mc.textRenderer, scoreText, width / 2, 100, 0xFFFFFFFF);
        }

        // Buttons
        int buttonWidth = 200;
        int buttonHeight = 20;
        int buttonX = width / 2 - buttonWidth / 2;
        int respawnY = height / 4 + 72;
        int titleScreenY = height / 4 + 96;

        int vOffset = data.buttonsActive ? 66 : 46;
        int textColor = data.buttonsActive ? 0xFFFFFF : 0xA0A0A0;

        // Respawn / Spectate World button
        drawVanillaButton(drawContext, buttonX, respawnY, buttonWidth, buttonHeight, vOffset);
        String respawnLabel = data.hardcore ? "Spectate World" : "Respawn";
        drawContext.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(respawnLabel), width / 2, respawnY + (buttonHeight - 8) / 2, textColor);

        // Title Screen button
        drawVanillaButton(drawContext, buttonX, titleScreenY, buttonWidth, buttonHeight, vOffset);
        drawContext.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("Title Screen"), width / 2, titleScreenY + (buttonHeight - 8) / 2, textColor);
    }

    private static void drawVanillaButton(DrawContext drawContext, int x, int y, int width, int height, int vOffset)
    {
        int halfWidth = width / 2;
        drawContext.drawTexture(WIDGETS, x, y, 0, vOffset, halfWidth, height);
        drawContext.drawTexture(WIDGETS, x + halfWidth, y, 200 - halfWidth, vOffset, halfWidth, height);
    }
}
