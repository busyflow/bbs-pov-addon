package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.data.EnchantmentSnapshot;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.EnchantingPhrases;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/** Enchanting-table clip renderer. Saved type id remains {@code enchanting_table}. */
public final class EnchantingGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final EnchantingGuiRenderer INSTANCE = new EnchantingGuiRenderer();

    private static final Identifier ENCHANTING_BOOK_TEXTURE = new Identifier("textures/entity/enchanting_table_book.png");
    private static final Identifier ENCHANTING_TABLE_TEXTURE = new Identifier("textures/gui/container/enchanting_table.png");
    private static BookModel enchantingBook;

    private EnchantingGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawEarlyChrome(GuiRenderContext ctx)
    {
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        drawBook(ctx.batcher, ctx.clip, ctx.localTick, ctx.opacity);
        drawOffers(ctx.batcher, ctx.clip, ctx.localTick, cursorX, cursorY, ctx.hover);
    }

    private static void drawBook(Batcher2D batcher, GuiPovActionClip clip, float tick, float opacity)
    {
        if (enchantingBook == null)
        {
            enchantingBook = new BookModel(MinecraftClient.getInstance()
                .getEntityModelLoader()
                .getModelPart(EntityModelLayers.BOOK));
        }

        float open = enchantingBookOpen(clip, tick);
        batcher.getContext().draw();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.method_34742();
        MatrixStack matrices = batcher.getContext().getMatrices();
        matrices.push();
        matrices.translate(33F, 31F, 100F);
        matrices.scale(-40F, 40F, 40F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(25F));
        float closed = 1F - open;
        matrices.translate(closed * 0.2F, closed * 0.1F, closed * 0.25F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-closed * 90F - 90F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));

        float page = open > 0F ? tick * 0.02F : 0F;
        float leftPage = MathHelper.clamp(MathHelper.fractionalPart(page + 0.25F) * 1.6F - 0.3F, 0F, 1F);
        float rightPage = MathHelper.clamp(MathHelper.fractionalPart(page + 0.75F) * 1.6F - 0.3F, 0F, 1F);
        enchantingBook.setPageAngles(0F, leftPage, rightPage, open);
        var vertexConsumer = batcher.getContext()
            .getVertexConsumers()
            .getBuffer(enchantingBook.getLayer(ENCHANTING_BOOK_TEXTURE));
        enchantingBook.render(
            matrices,
            vertexConsumer,
            15728880,
            OverlayTexture.DEFAULT_UV,
            1F,
            1F,
            1F,
            opacity);
        batcher.getContext().draw();
        matrices.pop();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static float enchantingBookOpen(GuiPovActionClip clip, float tick)
    {
        if (clip.enchantBookOpen != null && !clip.enchantBookOpen.isEmpty())
        {
            return MathHelper.clamp(clip.enchantBookOpen.interpolate(tick, 0F), 0F, 1F);
        }

        int[][] offers = EnchantmentSnapshot.parse(GuiTextRenderer.sampleString(clip.enchantOffers, tick, ""));
        for (int[] offer : offers)
        {
            if (offer[0] > 0)
            {
                return 1F;
            }
        }
        if (!clip.enchantOffers.isEmpty())
        {
            return 0F;
        }
        ItemStack item = GuiSlotRenderer.sampleSlot(clip, "enchanting_table", "item", tick);
        return item.isEmpty() || !item.isEnchantable() ? 0F : 1F;
    }

    private static void drawOffers(
        Batcher2D batcher,
        GuiPovActionClip clip,
        float tick,
        float cursorX,
        float cursorY,
        GuiPointerHover hover)
    {
        int[][] offers = EnchantmentSnapshot.parse(GuiTextRenderer.sampleString(clip.enchantOffers, tick, ""));
        int seed = GuiTextRenderer.sampleInt(clip.enchantSeed, tick, 0);
        int playerLevel = GuiTextRenderer.sampleInt(clip.enchantPlayerLevel, tick, 0);
        boolean creative = GuiTextRenderer.sampleBool(clip.enchantCreative, tick, false);
        int lapis = GuiSlotRenderer.sampleSlot(clip, "enchanting_table", "lapis", tick).getCount();
        var font = MinecraftClient.getInstance().textRenderer;
        EnchantingPhrases.getInstance().setSeed(seed);

        for (int i = 0; i < 3; i++)
        {
            int power = offers[i][0];
            int rowX = 60;
            int rowY = 14 + 19 * i;
            if (power <= 0)
            {
                batcher.getContext().drawTexture(ENCHANTING_TABLE_TEXTURE, rowX, rowY, 0, 185, 108, 19);
                continue;
            }

            String cost = Integer.toString(power);
            int phraseWidth = 86 - font.getWidth(cost);
            StringVisitable phrase = EnchantingPhrases.getInstance().generatePhrase(font, phraseWidth);
            boolean affordable = creative || (lapis >= i + 1 && playerLevel >= power);
            boolean hovered = cursorX >= rowX && cursorY >= rowY && cursorX < rowX + 108 && cursorY < rowY + 19;
            if (!affordable)
            {
                batcher.getContext().drawTexture(ENCHANTING_TABLE_TEXTURE, rowX, rowY, 0, 185, 108, 19);
                batcher.getContext().drawTexture(ENCHANTING_TABLE_TEXTURE, rowX + 1, rowY + 1, 16 * i, 239, 16, 16);
                batcher.getContext().drawTextWrapped(font, phrase, rowX + 20, rowY + 2, phraseWidth, (6839882 & 16711422) >> 1);
                batcher.getContext().drawTextWithShadow(
                    font,
                    cost,
                    rowX + 20 + 86 - font.getWidth(cost),
                    rowY + 2,
                    4226832);
            }
            else
            {
                int v = hovered ? 204 : 166;
                batcher.getContext().drawTexture(
                    ENCHANTING_TABLE_TEXTURE,
                    rowX,
                    rowY,
                    0,
                    v,
                    108,
                    19);
                int textColor = hovered ? 16777088 : 6839882;
                batcher.getContext().drawTexture(ENCHANTING_TABLE_TEXTURE, rowX + 1, rowY + 1, 16 * i, 223, 16, 16);
                batcher.getContext().drawTextWrapped(font, phrase, rowX + 20, rowY + 2, phraseWidth, textColor);
                int costColor = hovered ? 16777088 : 8453920;
                batcher.getContext().drawTextWithShadow(
                    font,
                    cost,
                    rowX + 20 + 86 - font.getWidth(cost),
                    rowY + 2,
                    costColor);
            }

            boolean tooltipBounds = cursorX >= rowX && cursorY >= rowY && cursorX < rowX + 108 && cursorY < rowY + 17;
            if (tooltipBounds)
            {
                List<Text> lines = enchantmentClueTooltip(offers[i][1], offers[i][2], power, i + 1, lapis, playerLevel, creative);
                if (!lines.isEmpty())
                {
                    hover.lines = lines;
                }
            }
        }
    }

    private static List<Text> enchantmentClueTooltip(
        int enchantmentId,
        int enchantmentLevel,
        int power,
        int lapisCost,
        int lapisCount,
        int playerLevel,
        boolean creative)
    {
        Enchantment enchantment = Enchantment.byRawId(enchantmentId);
        if (enchantment == null || enchantmentLevel < 0 || power <= 0)
        {
            return List.of();
        }

        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("container.enchant.clue", enchantment.getName(enchantmentLevel)).formatted(Formatting.WHITE));
        if (creative)
        {
            return lines;
        }

        lines.add(ScreenTexts.EMPTY);
        if (playerLevel < power)
        {
            lines.add(Text.translatable("container.enchant.level.requirement", power).formatted(Formatting.RED));
            return lines;
        }

        lines.add((lapisCost == 1
            ? Text.translatable("container.enchant.lapis.one")
            : Text.translatable("container.enchant.lapis.many", lapisCost))
            .formatted(lapisCount >= lapisCost ? Formatting.GRAY : Formatting.RED));
        lines.add((lapisCost == 1
            ? Text.translatable("container.enchant.level.one")
            : Text.translatable("container.enchant.level.many", lapisCost))
            .formatted(Formatting.GRAY));
        return lines;
    }

}
