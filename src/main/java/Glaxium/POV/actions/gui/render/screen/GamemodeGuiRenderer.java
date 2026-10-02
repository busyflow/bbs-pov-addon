package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Gamemode-switcher clip renderer. Saved type id remains {@code gamemode_switcher}. */
public final class GamemodeGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final GamemodeGuiRenderer INSTANCE = new GamemodeGuiRenderer();

    private static final Identifier GAMEMODE_TEXTURE = new Identifier("textures/gui/container/gamemode_switcher.png");
    private static final ItemStack GRASS_BLOCK_STACK = new ItemStack(net.minecraft.item.Items.GRASS_BLOCK);
    private static final ItemStack IRON_SWORD_STACK = new ItemStack(net.minecraft.item.Items.IRON_SWORD);
    private static final ItemStack MAP_STACK = new ItemStack(net.minecraft.item.Items.MAP);
    private static final ItemStack ENDER_EYE_STACK = new ItemStack(net.minecraft.item.Items.ENDER_EYE);

    private static void drawSwitcher(
        Batcher2D batcher,
        GuiPovActionClip clip,
        float tick,
        float originX,
        float originY,
        float scaleX,
        float scaleY)
    {
        int selectedMode = clip.gamemodeSelection.isEmpty() ? 0 : clip.gamemodeSelection.interpolate(tick, 0);
        selectedMode = MathHelper.clamp(selectedMode, 0, 3);

        Text title;
        int highlightedSlotIndex;
        // 0 = Survival, 1 = Creative, 2 = Adventure, 3 = Spectator
        switch (selectedMode)
        {
            case 1 -> {
                title = Text.translatable("gameMode.creative");
                highlightedSlotIndex = 0;
            }
            case 2 -> {
                title = Text.translatable("gameMode.adventure");
                highlightedSlotIndex = 2;
            }
            case 3 -> {
                title = Text.translatable("gameMode.spectator");
                highlightedSlotIndex = 3;
            }
            default -> {
                title = Text.translatable("gameMode.survival");
                highlightedSlotIndex = 1;
            }
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        DrawContext context = batcher.getContext();
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(originX, originY, 0F);
        matrices.scale(scaleX, scaleY, 1F);

        // 1. Draw top Title Text centered
        context.drawCenteredTextWithShadow(textRenderer, title, 125 / 2, 7, 0xFFFFFF);

        // 2. Draw 4 Slot Buttons & Item Icons
        // 4 slots: 0 = Creative (Grass), 1 = Survival (Sword), 2 = Adventure (Map), 3 = Spectator (Eye)
        ItemStack[] icons = new ItemStack[] { GRASS_BLOCK_STACK, IRON_SWORD_STACK, MAP_STACK, ENDER_EYE_STACK };

        for (int i = 0; i < 4; i++)
        {
            int slotX = 3 + i * 31;
            int slotY = 27;
            boolean highlighted = (i == highlightedSlotIndex);

            context.drawTexture(GAMEMODE_TEXTURE, slotX, slotY, 0, 75, 26, 26, 128, 128);
            if (highlighted)
            {
                context.drawTexture(GAMEMODE_TEXTURE, slotX, slotY, 26, 75, 26, 26, 128, 128);
            }

            DiffuseLighting.enableGuiDepthLighting();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            GuiSlotRenderer.drawSlotItem(batcher, icons[i], slotX + 5, slotY + 5);
            batcher.flush();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            DiffuseLighting.disableGuiDepthLighting();
        }

        // 3. Draw bottom "[ F4 ] Next" text centered
        Text keyText = Text.literal("[ F4 ]").formatted(Formatting.AQUA);
        Text hintText = Text.translatable("debug.gamemodes.select_next", keyText);
        context.drawCenteredTextWithShadow(textRenderer, hintText, 125 / 2, 63, 0xFFFFFF);

        matrices.pop();
        batcher.flush();
    }


    private GamemodeGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawPreview(GuiRenderContext ctx)
    {
        drawSwitcher(ctx.batcher, ctx.clip, ctx.localTick, ctx.originX, ctx.originY, ctx.scaleX, ctx.scaleY);
    }
}
