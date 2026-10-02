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

    private static final Identifier TEXT_FIELD = new Identifier("container/anvil/text_field");
    private static final Identifier TEXT_FIELD_DISABLED = new Identifier("container/anvil/text_field_disabled");
    private static final Identifier ERROR = new Identifier("container/anvil/error");

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
        ctx.batcher.getContext().drawGuiTexture(
            hasInput ? TEXT_FIELD : TEXT_FIELD_DISABLED,
            59,
            20,
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
            ctx.batcher.getContext().drawGuiTexture(ERROR, 99, 45, 28, 21);
        }
    }
}
