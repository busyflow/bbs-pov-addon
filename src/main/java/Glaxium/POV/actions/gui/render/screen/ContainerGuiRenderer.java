package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiRecipeBookRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;

/**
 * Generic containers plus survival inventory / crafting-table recipe buttons.
 * Saved type ids are unchanged.
 */
public final class ContainerGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final ContainerGuiRenderer INSTANCE = new ContainerGuiRenderer();

    private ContainerGuiRenderer()
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
        if ("inventory".equals(ctx.guiId))
        {
            GuiRecipeBookRenderer.drawRecipeButton(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, 104, 61, cursorX, cursorY);
        }
        else if ("crafting_table".equals(ctx.guiId))
        {
            GuiRecipeBookRenderer.drawRecipeButton(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, 5, 34, cursorX, cursorY);
        }
    }

    @Override
    public void drawPreview(GuiRenderContext ctx)
    {
        if ("inventory".equals(ctx.guiId))
        {
            GuiEntityPreviewRenderer.drawInventoryPreview(ctx, false);
        }
    }
}
