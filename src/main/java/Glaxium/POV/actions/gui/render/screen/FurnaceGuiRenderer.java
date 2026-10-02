package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiRecipeBookRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Furnace / blast furnace / smoker clip renderer. Saved type ids are unchanged. */
public final class FurnaceGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final FurnaceGuiRenderer INSTANCE = new FurnaceGuiRenderer();

    private static final Identifier FURNACE_TEXTURE = new Identifier("textures/gui/container/furnace.png");
    private static final Identifier BLAST_FURNACE_TEXTURE = new Identifier("textures/gui/container/blast_furnace.png");
    private static final Identifier SMOKER_TEXTURE = new Identifier("textures/gui/container/smoker.png");

    private FurnaceGuiRenderer()
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
        GuiRecipeBookRenderer.drawRecipeButton(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, 20, 34, cursorX, cursorY);
        drawProgress(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick);
    }

    private static void drawProgress(Batcher2D batcher, GuiPovActionClip clip, String guiId, float tick)
    {
        float lit = MathHelper.clamp(GuiTextRenderer.sampleFloat(clip.getFurnaceLit(guiId), tick, 0F), 0F, 1F);
        float cook = MathHelper.clamp(GuiTextRenderer.sampleFloat(clip.getFurnaceCook(guiId), tick, 0F), 0F, 1F);
        Identifier bgTexture = switch (guiId)
        {
            case "blast_furnace" -> BLAST_FURNACE_TEXTURE;
            case "smoker" -> SMOKER_TEXTURE;
            default -> FURNACE_TEXTURE;
        };

        if (lit > 0F)
        {
            int fireH = MathHelper.ceil(lit * 13F) + 1;
            batcher.getContext().drawTexture(
                bgTexture,
                56,
                36 + 14 - fireH,
                176,
                14 - fireH,
                14,
                fireH);
        }
        if (cook > 0F)
        {
            int arrowW = MathHelper.ceil(cook * 24F);
            batcher.getContext().drawTexture(
                bgTexture,
                79,
                34,
                176,
                14,
                arrowW,
                16);
        }
    }

}
