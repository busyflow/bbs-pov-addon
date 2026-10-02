package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Brewing-stand clip renderer. Saved type id remains {@code brewing_stand}. */
public final class BrewingGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final BrewingGuiRenderer INSTANCE = new BrewingGuiRenderer();

    private static final Identifier BREWING_FUEL = new Identifier("container/brewing_stand/fuel_length");
    private static final Identifier BREWING_ARROW = new Identifier("container/brewing_stand/brew_progress");
    private static final Identifier BREWING_BUBBLES = new Identifier("container/brewing_stand/bubbles");
    private static final int[] BREWING_BUBBLE_FRAMES = {0, 6, 11, 16, 20, 24, 29};

    private BrewingGuiRenderer()
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
        drawProgress(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick);
    }

    private static void drawProgress(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick)
    {
        float fuel = MathHelper.clamp(GuiTextRenderer.sampleFloat(clip.getBrewFuel(guiId), tick, 0F), 0F, 1F);
        float brew = MathHelper.clamp(GuiTextRenderer.sampleFloat(clip.getBrewProgress(guiId), tick, 0F), 0F, 1F);
        boolean bubbles = GuiTextRenderer.sampleBool(clip.getBrewBubbles(guiId), tick, false);

        int fuelW = MathHelper.clamp(Math.round(fuel * 18F), 0, 18);
        if (fuelW > 0)
        {
            batcher.getContext().drawGuiTexture(BREWING_FUEL, 18, 4, 0, 0, 60, 44, fuelW, 4);
        }

        int arrowH = MathHelper.clamp(Math.round(brew * 28F), 0, 28);
        if (arrowH > 0)
        {
            batcher.getContext().drawGuiTexture(BREWING_ARROW, 9, 28, 0, 0, 97, 16, 9, arrowH);
        }

        if (bubbles)
        {
            int frame = Math.floorMod((int) tick / 2, BREWING_BUBBLE_FRAMES.length);
            int bubbleH = BREWING_BUBBLE_FRAMES[frame];
            if (bubbleH > 0)
            {
                batcher.getContext().drawGuiTexture(
                    BREWING_BUBBLES,
                    12,
                    29,
                    0,
                    29 - bubbleH,
                    63,
                    14 + 29 - bubbleH,
                    12,
                    bubbleH);
            }
        }
    }


}
