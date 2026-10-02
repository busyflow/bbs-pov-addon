package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Shared GUI clip layout: dim, background, chrome, slots, recipe book, cursor. */
public final class GuiStandardLayoutRenderer
{
    private GuiStandardLayoutRenderer()
    {
    }

    public static void render(GuiRenderContext ctx, GuiScreenChrome chrome)
    {
        if (chrome == null)
        {
            chrome = GuiScreenChrome.EMPTY;
        }

        MatrixStack matrices = ctx.matrices;
        Batcher2D batcher = ctx.batcher;
        float globalTick = ctx.globalTick;
        float localTick = ctx.localTick;
        int screenWidth = ctx.screenWidth;
        int screenHeight = ctx.screenHeight;
        GuiTypeEntry entry = ctx.entry;
        String guiId = ctx.guiId;
        float scaleX = ctx.scaleX;
        float scaleY = ctx.scaleY;
        float opacity = ctx.opacity;
        float bgOpacity = ctx.bgOpacity;
        float originX = ctx.originX;
        float originY = ctx.originY;
        boolean recipeOpen = ctx.recipeOpen;
        boolean cursorVisible = ctx.cursorVisible;
        float cursorGuiX = ctx.cursorGuiX;
        float cursorGuiY = ctx.cursorGuiY;
        GuiPointerHover pointerHover = ctx.hover;

        // HUD items write depth at z≈150; GUI_OVERLAY always passes so the dim covers them.
        int topAlpha = (int) (0xC0 * opacity * bgOpacity);
        int bottomAlpha = (int) (0xD0 * opacity * bgOpacity);
        if (!ctx.skipEntityPreview && bottomAlpha > 0)
        {
            int topColor = (topAlpha << 24) | 0x101010;
            int bottomColor = (bottomAlpha << 24) | 0x101010;
            batcher.flush();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            batcher.getContext().fillGradient(
                RenderLayer.getGuiOverlay(),
                0,
                0,
                screenWidth,
                screenHeight,
                topColor,
                bottomColor,
                0);
            batcher.flush();
        }
        batcher.flush();
        matrices.push();
        matrices.translate(originX, originY, 0F);
        if (ctx.transform != null && ctx.transform.rotate.z != 0F)
        {
            float pivotX = (entry.regionWidth * scaleX) / 2F;
            float pivotY = (entry.regionHeight * scaleY) / 2F;
            matrices.translate(pivotX, pivotY, 0F);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(ctx.transform.rotate.z));
            matrices.translate(-pivotX, -pivotY, 0F);
        }
        matrices.scale(scaleX, scaleY, 1F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        batcher.getContext().setShaderColor(1F, 1F, 1F, opacity);
        RenderSystem.setShaderColor(1F, 1F, 1F, opacity);

        chrome.drawBackground(ctx);

        chrome.drawEarlyChrome(ctx);

        if (recipeOpen)
        {
            GuiRecipeBookRenderer.render(
                batcher,
                ctx.clip,
                ctx.replayKeyframes,
                guiId,
                localTick,
                globalTick,
                originX,
                originY,
                scaleX,
                scaleY,
                screenWidth,
                screenHeight,
                opacity,
                cursorVisible ? cursorGuiX : -1000F,
                cursorVisible ? cursorGuiY : -1000F,
                pointerHover);
        }

        batcher.getContext().draw();

        int titleColor = ((int) (0xFF * opacity) << 24) | 0x404040;
        boolean drewTitle = false;
        net.minecraft.text.Text titleText = entry.getTitleText();
        String titleStr = titleText != null ? titleText.getString() : "";
        if (!titleStr.isBlank() && !"villager".equals(entry.id))
        {
            int tx = entry.titleX;
            if (tx < 0)
            {
                tx = (entry.regionWidth - MinecraftClient.getInstance().textRenderer.getWidth(titleStr)) / 2;
            }
            batcher.text(titleStr, tx, entry.titleY, titleColor, false);
            drewTitle = true;
        }
        if (entry.inventoryTitleX >= 0)
        {
            String invText = net.minecraft.text.Text.translatable("container.inventory").getString();
            batcher.text(invText, entry.inventoryTitleX, entry.inventoryTitleY, titleColor, false);
            drewTitle = true;
        }
        if (drewTitle)
        {
            batcher.flush();
        }

        // GUI item models require depth writes or rear faces draw over their fronts.
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.enableGuiDepthLighting();

        ItemStack hoveredStack = chrome.renderItems(ctx);

        chrome.drawLateItems(ctx);
        if (recipeOpen)
        {
            GuiRecipeBookRenderer.renderItems(
                batcher,
                ctx.clip,
                ctx.replayKeyframes,
                guiId,
                localTick,
                globalTick,
                originX,
                originY,
                scaleX,
                scaleY,
                screenWidth,
                screenHeight,
                cursorVisible ? cursorGuiX : -1000F,
                cursorVisible ? cursorGuiY : -1000F,
                pointerHover);
        }
        if (recipeOpen)
        {
            ItemStack ghostHover = GuiRecipeBookRenderer.renderGhosts(
                batcher,
                ctx.clip,
                ctx.replayKeyframes,
                guiId,
                localTick,
                globalTick,
                opacity,
                cursorVisible ? cursorGuiX : -1000F,
                cursorVisible ? cursorGuiY : -1000F);
            if (pointerHover.item == null && ghostHover != null && !ghostHover.isEmpty())
            {
                pointerHover.item = ghostHover;
                pointerHover.itemGroup = false;
            }
        }
        if (pointerHover.item != null && !pointerHover.item.isEmpty())
        {
            hoveredStack = pointerHover.item;
            pointerHover.itemGroup = false;
        }

        chrome.drawPreview(ctx);

        batcher.flush();
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        matrices.pop();

        GuiCursorRenderer.renderTooltips(ctx, hoveredStack);

        batcher.getContext().setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
