package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

/** Cartography-table clip renderer. Saved type id remains {@code cartography_table}. */
public final class CartographyGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final CartographyGuiRenderer INSTANCE = new CartographyGuiRenderer();

    private static final Identifier CARTOGRAPHY_TEXTURE = new Identifier("textures/gui/container/cartography_table.png");

    private CartographyGuiRenderer()
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
        drawChrome(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick);
    }

    private static void drawChrome(Batcher2D batcher, GuiPovActionClip clip, String guiId, float tick)
    {
        KeyframeChannel<ItemStack> additionChannel = clip.getGuiSlot(guiId, "addition");
        ItemStack additionStack = additionChannel == null || additionChannel.isEmpty()
            ? ItemStack.EMPTY
            : additionChannel.interpolate(tick, ItemStack.EMPTY);

        KeyframeChannel<ItemStack> mapChannel = clip.getGuiSlot(guiId, "map");
        ItemStack mapStack = mapChannel == null || mapChannel.isEmpty()
            ? ItemStack.EMPTY
            : mapChannel.interpolate(tick, ItemStack.EMPTY);

        boolean isClone = additionStack != null && !additionStack.isEmpty() && (additionStack.isOf(Items.MAP) || additionStack.isOf(Items.FILLED_MAP) || additionStack.isOf(Items.PAPER));
        boolean isGlassPane = additionStack != null && !additionStack.isEmpty() && (additionStack.isOf(Items.GLASS_PANE) || (additionStack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof net.minecraft.block.PaneBlock));

        if (isClone)
        {
            batcher.getContext().drawTexture(CARTOGRAPHY_TEXTURE, 67 + 16, 13, 176, 132, 50, 66);
            batcher.getContext().drawTexture(CARTOGRAPHY_TEXTURE, 67, 13 + 16, 176, 132, 50, 66);
        }
        else if (isGlassPane)
        {
            batcher.getContext().drawTexture(CARTOGRAPHY_TEXTURE, 67, 13, 176, 0, 66, 66);
            batcher.getContext().drawTexture(CARTOGRAPHY_TEXTURE, 66, 12, 0, 166, 66, 66);
        }
        else
        {
            batcher.getContext().drawTexture(CARTOGRAPHY_TEXTURE, 67, 13, 176, 0, 66, 66);
        }
    }

    @Override
    public void drawLateItems(GuiRenderContext ctx)
    {
    }
}
