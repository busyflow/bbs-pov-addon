package Glaxium.POV.actions.gui.editor;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.render.GuiActionRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class UIGuiTypeList extends UIList<GuiTypeEntry>
{
    public static final int ITEM_HEIGHT = 48;
    private static final GuiPovActionClip PREVIEW_CLIP = new GuiPovActionClip();

    static
    {
        PREVIEW_CLIP.tick.set(0);
        PREVIEW_CLIP.duration.set(100);
        PREVIEW_CLIP.enchantBookOpen.insert(0F, 0F);
        PREVIEW_CLIP.enchantPlayerLevel.insert(0F, 0);
        PREVIEW_CLIP.enchantSeed.insert(0F, 0);
        PREVIEW_CLIP.enchantOffers.insert(0F, "");
        PREVIEW_CLIP.gamemodeSelection.insert(0F, 0);
        PREVIEW_CLIP.creativeTab.insert(0F, 0);
        PREVIEW_CLIP.creativePage.insert(0F, 0);
        PREVIEW_CLIP.creativeScroll.insert(0F, 0F);
        PREVIEW_CLIP.creativeRow.insert(0F, 0);
        PREVIEW_CLIP.anvilName.insert(0F, "");
    }

    public UIGuiTypeList(Consumer<List<GuiTypeEntry>> callback)
    {
        super(callback);
        this.scroll.scrollItemSize = ITEM_HEIGHT;
    }

    private static <T> void setChannel(KeyframeChannel<T> channel, T value)
    {
        if (channel == null)
        {
            return;
        }
        if (channel.isEmpty())
        {
            channel.insert(0F, value);
        }
        else
        {
            channel.getKeyframes().get(0).setValue(value);
        }
    }

    @Override
    protected String elementToString(UIContext context, int index, GuiTypeEntry element)
    {
        return element == null ? "" : (element.name + " " + element.id + " " + element.category);
    }

    @Override
    protected void renderElementPart(
        UIContext context,
        GuiTypeEntry element,
        int i,
        int x,
        int y,
        boolean hover,
        boolean selected)
    {
        if (element == null)
        {
            return;
        }

        int boxX = x + 4;
        int boxY = y + 3;
        int boxW = 60;
        int boxH = 42;
        context.batcher.box(boxX, boxY, boxX + boxW, boxY + boxH, 0x88000000);
        context.batcher.outline(boxX, boxY, boxX + boxW, boxY + boxH, selected ? 0xccffffff : 0x44ffffff);

        // 2. Render full featured GUI thumbnail preview
        this.renderPreview(context, element, boxX, boxY, boxW, boxH);

        // 3. Draw larger, centered 3D block/item icon with proper depth buffer, lighting & culling
        int iconX = boxX + boxW + 10;
        int iconY = y + 13;
        float iconScale = 1.35F;

        ItemStack itemIcon = element.getIcon();
        if (itemIcon != null && !itemIcon.isEmpty())
        {
            try
            {
                context.batcher.flush();
                MatrixStack matrices = context.batcher.getContext().getMatrices();
                matrices.push();
                matrices.translate(iconX, iconY, 100F);
                matrices.scale(iconScale, iconScale, 1F);

                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.enableCull();
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

                DiffuseLighting.enableGuiDepthLighting();
                context.batcher.getContext().drawItem(itemIcon, 0, 0);
                context.batcher.getContext().draw();
                DiffuseLighting.disableGuiDepthLighting();

                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();

                matrices.pop();
            }
            catch (Exception ignored)
            {
            }
        }

        // 4. Right side labels: Title, Category Badge, Resource ID
        int textX = iconX + 28;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        int titleColor = selected ? 0xffffff : (hover ? 0xfff0aa : 0xffffff);
        context.batcher.textShadow(element.name, textX, y + 10, titleColor);

        // Category Badge Pill
        String catText = element.category != null ? element.category : "GUI";
        int catColor = GuiTypeEntry.getCategoryColor(element.category);
        int nameWidth = textRenderer.getWidth(element.name);
        int badgeX = textX + nameWidth + 6;
        int badgeY = y + 9;
        int badgeW = textRenderer.getWidth(catText) + 6;
        int badgeH = 10;

        context.batcher.box(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, (catColor & 0x00ffffff) | 0x33000000);
        context.batcher.outline(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, (catColor & 0x00ffffff) | 0x99000000);
        context.batcher.text(catText, badgeX + 3, badgeY + 1, catColor);

        // Second line: Resource ID
        int subtitleColor = selected ? 0xd0e8ff : (hover ? 0xb0b0b0 : 0x888888);
        context.batcher.textShadow("minecraft:" + element.id, textX, y + 25, subtitleColor);
    }

    private void renderPreview(UIContext context, GuiTypeEntry element, int boxX, int boxY, int boxW, int boxH)
    {
        int innerPad = 2;
        int maxW = boxW - (innerPad * 2);
        int maxH = boxH - (innerPad * 2);
        int regW = Math.max(1, element.regionWidth);
        int regH = Math.max(1, element.regionHeight);
        /* Tabs sit at y=-28 and y=132 (32px), so the real footprint is 195x192. */
        if ("creative_inventory".equals(element.id))
        {
            regW = 195;
            regH = 192;
        }
        float scale = Math.min((float) maxW / regW, (float) maxH / regH);

        int drawW = Math.max(1, Math.round(regW * scale));
        int drawH = Math.max(1, Math.round(regH * scale));
        int drawX = boxX + innerPad + (maxW - drawW) / 2;
        int drawY = boxY + innerPad + (maxH - drawH) / 2;

        setChannel(PREVIEW_CLIP.state, element.id);
        setChannel(PREVIEW_CLIP.getDarknessOpacity(element.id), 0F);
        setChannel(PREVIEW_CLIP.getCursorVisible(element.id), false);
        setChannel(PREVIEW_CLIP.getOpacity(element.id), 1F);
        setChannel(PREVIEW_CLIP.getFurnaceLit(element.id), 0F);
        setChannel(PREVIEW_CLIP.getFurnaceCook(element.id), 0F);
        setChannel(PREVIEW_CLIP.getBrewProgress(element.id), 0F);
        setChannel(PREVIEW_CLIP.getBrewFuel(element.id), 0F);
        setChannel(PREVIEW_CLIP.getBrewBubbles(element.id), false);
        setChannel(PREVIEW_CLIP.anvilName, "");
        setChannel(PREVIEW_CLIP.getLayout(element.id), new Transform());

        MatrixStack matrices = context.batcher.getContext().getMatrices();
        matrices.push();
        matrices.translate(drawX + drawW / 2F, drawY + drawH / 2F, 0F);
        matrices.scale(scale, scale, 1F);
        matrices.translate(-regW / 2F, -regH / 2F, 0F);

        context.batcher.flush();
        context.batcher.getContext().enableScissor(boxX, boxY, boxX + boxW, boxY + boxH);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);

        try
        {
            GuiActionRenderer.renderGuiClip(
                matrices,
                context.batcher,
                null,
                PREVIEW_CLIP,
                null,
                0F,
                regW,
                regH,
                false,
                true
            );
            context.batcher.getContext().draw();
        }
        catch (Exception ignored)
        {
        }
        finally
        {
            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            context.batcher.flush();
            context.batcher.getContext().disableScissor();
            matrices.pop();
        }
    }
}
