package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Beacon screen clip renderer. Saved type id remains {@code beacon}. */
public final class BeaconGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final BeaconGuiRenderer INSTANCE = new BeaconGuiRenderer();

    private static final Identifier BEACON_TEXTURE = new Identifier("textures/gui/container/beacon.png");

    private BeaconGuiRenderer()
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
        drawChrome(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, cursorX, cursorY, ctx.hover);
    }

    @Override
    public void drawLateItems(GuiRenderContext ctx)
    {
        drawPaymentIcons(ctx.batcher);
    }

    private static void drawChrome(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float localTick,
        float cursorX,
        float cursorY,
        GuiPointerHover hover)
    {
        batcher.flush();
        DrawContext context = batcher.getContext();
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        // Draw Centered Titles
        net.minecraft.text.Text primaryText = net.minecraft.text.Text.translatable("block.minecraft.beacon.primary");
        net.minecraft.text.Text secondaryText = net.minecraft.text.Text.translatable("block.minecraft.beacon.secondary");
        int pw = textRenderer.getWidth(primaryText);
        int sw = textRenderer.getWidth(secondaryText);
        context.drawTextWithShadow(textRenderer, primaryText, 62 - pw / 2, 10, 0xE0E0E0);
        context.drawTextWithShadow(textRenderer, secondaryText, 169 - sw / 2, 10, 0xE0E0E0);

        context.getMatrices().push();
        context.getMatrices().translate(0F, 0F, 10F);

        // Power Buttons
        int primary = clip.beaconPrimary.isEmpty() ? 0 : clip.beaconPrimary.interpolate(localTick, 0);
        int secondary = clip.beaconSecondary.isEmpty() ? 0 : clip.beaconSecondary.interpolate(localTick, 0);
        int level = clip.beaconLevel.isEmpty() ? 0 : clip.beaconLevel.interpolate(localTick, 0);
        KeyframeChannel<ItemStack> paymentChannel = clip.getGuiSlot(guiId, "payment");
        ItemStack payment = paymentChannel == null || paymentChannel.isEmpty()
            ? ItemStack.EMPTY : paymentChannel.interpolate(localTick, ItemStack.EMPTY);
        boolean hasPayment = payment != null && !payment.isEmpty();
        drawBeaconButton(context, 58, 27, StatusEffects.SPEED, level >= 1, primary == 1, cursorX, cursorY, hover);
        drawBeaconButton(context, 80, 27, StatusEffects.HASTE, level >= 1, primary == 2, cursorX, cursorY, hover);
        drawBeaconButton(context, 58, 51, StatusEffects.RESISTANCE, level >= 2, primary == 3, cursorX, cursorY, hover);
        drawBeaconButton(context, 80, 51, StatusEffects.JUMP_BOOST, level >= 2, primary == 4, cursorX, cursorY, hover);
        drawBeaconButton(context, 69, 75, StatusEffects.STRENGTH, level >= 3, primary == 5, cursorX, cursorY, hover);
        drawBeaconButton(context, 144, 47, StatusEffects.REGENERATION, level >= 4, secondary == 1, cursorX, cursorY, hover);
        StatusEffect primaryEffect = beaconPrimaryEffect(primary);
        if (level >= 4 && primaryEffect != null)
        {
            drawBeaconButton(context, 168, 47, primaryEffect, true, secondary == 2, cursorX, cursorY, hover);
        }

        // Confirm (Done) and Cancel (X) buttons
        boolean confirmHover = GuiTextRenderer.inBounds(cursorX, cursorY, 164, 107, 22, 22);
        boolean cancelHover = GuiTextRenderer.inBounds(cursorX, cursorY, 190, 107, 22, 22);

        // Vanilla states: normal=0, selected=22, inactive=44, hovered=66.
        int confirmU = (!hasPayment || primary == 0) ? 44 : (confirmHover ? 66 : 0);
        int cancelU = cancelHover ? 66 : 0;
        context.drawTexture(BEACON_TEXTURE, 164, 107, confirmU, 219, 22, 22);
        context.drawTexture(BEACON_TEXTURE, 190, 107, cancelU, 219, 22, 22);

        // In 1.20.1, confirm and cancel icons are on beacon.png UV strip
        context.setShaderColor(!hasPayment || primary == 0 ? 0.45F : 1F, !hasPayment || primary == 0 ? 0.45F : 1F, !hasPayment || primary == 0 ? 0.45F : 1F, 1F);
        context.drawTexture(BEACON_TEXTURE, 166, 109, 90, 220, 18, 18);
        context.setShaderColor(1F, 1F, 1F, 1F);
        context.drawTexture(BEACON_TEXTURE, 192, 109, 112, 220, 18, 18);

        context.getMatrices().pop();
        context.draw();
        batcher.flush();
    }

    private static void drawBeaconButton(
        DrawContext context,
        int x,
        int y,
        StatusEffect effect,
        boolean enabled,
        boolean selected,
        float cursorX,
        float cursorY,
        GuiPointerHover hover)
    {
        boolean isHovered = GuiTextRenderer.inBounds(cursorX, cursorY, x, y, 22, 22);
        int u = !enabled ? 44 : (selected ? 22 : (isHovered ? 66 : 0));
        context.drawTexture(BEACON_TEXTURE, x, y, u, 219, 22, 22);
        if (effect != null)
        {
            Sprite sprite = MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(effect);
            if (sprite != null)
            {
                context.drawSprite(x + 2, y + 2, 0, 18, 18, sprite);
            }
            if (isHovered)
            {
                hover.widget = effect.getName();
            }
        }
    }

    private static StatusEffect beaconPrimaryEffect(int primary)
    {
        return switch (primary)
        {
            case 1 -> StatusEffects.SPEED;
            case 2 -> StatusEffects.HASTE;
            case 3 -> StatusEffects.RESISTANCE;
            case 4 -> StatusEffects.JUMP_BOOST;
            case 5 -> StatusEffects.STRENGTH;
            default -> null;
        };
    }

    private static void drawPaymentIcons(Batcher2D batcher)
    {
        DrawContext context = batcher.getContext();
        DiffuseLighting.enableGuiDepthLighting();
        context.getMatrices().push();
        context.getMatrices().translate(0F, 0F, 100F);
        context.drawItem(new ItemStack(net.minecraft.item.Items.NETHERITE_INGOT), 20, 109);
        context.drawItem(new ItemStack(net.minecraft.item.Items.EMERALD), 41, 109);
        context.drawItem(new ItemStack(net.minecraft.item.Items.DIAMOND), 63, 109);
        context.drawItem(new ItemStack(net.minecraft.item.Items.GOLD_INGOT), 86, 109);
        context.drawItem(new ItemStack(net.minecraft.item.Items.IRON_INGOT), 108, 109);
        context.getMatrices().pop();
    }

}
