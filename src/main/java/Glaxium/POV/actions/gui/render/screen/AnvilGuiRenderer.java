package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import net.minecraft.util.Identifier;

/** Anvil clip renderer. Saved type id remains {@code anvil}. */
public final class AnvilGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final AnvilGuiRenderer INSTANCE = new AnvilGuiRenderer();

    private static final Identifier ANVIL_TEXTURE = new Identifier("textures/gui/container/anvil.png");

    private AnvilGuiRenderer()
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
        boolean hasInput = !GuiSlotRenderer.isSlotEmpty(ctx.clip, ctx.guiId, "input_0", ctx.localTick);
        ctx.batcher.getContext().drawTexture(
            ANVIL_TEXTURE,
            59,
            20,
            0,
            hasInput ? 166 : 182,
            110,
            16);
        String name = GuiTextRenderer.sampleString(ctx.clip.anvilName, ctx.localTick, "");
        boolean focused = !hasInput || GuiTextRenderer.sampleBool(ctx.clip.anvilNameFocus, ctx.localTick, false);
        GuiTextRenderer.drawSearchField(
            ctx.batcher,
            name,
            62,
            24,
            103,
            12,
            focused,
            false,
            ctx.opacity,
            GuiTextRenderer.sampleInt(ctx.clip.anvilNameSelStart, ctx.localTick, name.length()),
            GuiTextRenderer.sampleInt(ctx.clip.anvilNameSelEnd, ctx.localTick, name.length()));
        if (GuiTextRenderer.sampleBool(ctx.clip.anvilError, ctx.localTick, false))
        {
            ctx.batcher.getContext().drawTexture(ANVIL_TEXTURE, 99, 45, 176, 0, 28, 21);
        }
    }
}
