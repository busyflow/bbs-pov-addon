package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.recording.GuiSlotDragPreview;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Set;

/** Shared GUI slot drawing, drag preview, and empty-slot placeholders. */
public final class GuiSlotRenderer
{
    private GuiSlotRenderer()
    {
    }

    public static ItemStack sampleSlot(
        GuiPovActionClip clip,
        String guiId,
        String slotId,
        float tick)
    {
        KeyframeChannel<ItemStack> channel = clip.getGuiSlot(guiId, slotId);
        ItemStack stack = channel == null || channel.isEmpty()
            ? ItemStack.EMPTY
            : channel.interpolate(tick, ItemStack.EMPTY);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static boolean isSlotEmpty(
        GuiPovActionClip clip,
        String guiId,
        String slotId,
        float tick)
    {
        KeyframeChannel<ItemStack> channel = clip.getGuiSlot(guiId, slotId);
        ItemStack stack = channel == null || channel.isEmpty()
            ? ItemStack.EMPTY
            : channel.interpolate(tick, ItemStack.EMPTY);
        return stack == null || stack.isEmpty();
    }

    public static void drawGuiSlotPlaceholder(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        String slotId,
        float tick,
        String spriteId,
        int x,
        int y,
        int size)
    {
        if (isSlotEmpty(clip, guiId, slotId, tick))
        {
            batcher.getContext().drawGuiTexture(new Identifier(spriteId), x, y, size, size);
        }
    }

    public static void drawCyclingSlotPlaceholder(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        String slotId,
        float tick,
        List<Identifier> sprites,
        int x,
        int y,
        float opacity)
    {
        if (!isSlotEmpty(clip, guiId, slotId, tick) || sprites.isEmpty())
        {
            return;
        }

        int cycle = Math.max(0, (int) Math.floor(tick / 30F));
        int current = cycle % sprites.size();

        if (tick >= 30F && sprites.size() > 1)
        {
            float fade = Math.min(tick % 30F, 4F) / 4F;
            int previous = (current + sprites.size() - 1) % sprites.size();
            drawBlockAtlasPlaceholder(batcher, sprites.get(previous), x, y, opacity * (1F - fade));
            drawBlockAtlasPlaceholder(batcher, sprites.get(current), x, y, opacity * fade);
        }
        else
        {
            drawBlockAtlasPlaceholder(batcher, sprites.get(current), x, y, opacity);
        }

        batcher.getContext().setShaderColor(1F, 1F, 1F, opacity);
        RenderSystem.setShaderColor(1F, 1F, 1F, opacity);
    }

    public static void drawBlockAtlasPlaceholder(
        Batcher2D batcher,
        Identifier spriteId,
        int x,
        int y,
        float alpha)
    {
        if (alpha <= 0F)
        {
            return;
        }

        var sprite = MinecraftClient.getInstance()
            .getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
            .apply(spriteId);
        batcher.getContext().drawSprite(x, y, 0, 16, 16, sprite, 1F, 1F, 1F, alpha);
        batcher.getContext().draw();
        batcher.getContext().setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static ItemStack processSlot(
        Batcher2D batcher,
        KeyframeChannel<ItemStack> channel,
        float localTick,
        int x,
        int y,
        float curX,
        float curY)
    {
        return processSlot(batcher, channel, localTick, x, y, curX, curY, false);
    }

    public static ItemStack processSlot(
        Batcher2D batcher,
        KeyframeChannel<ItemStack> channel,
        float localTick,
        int x,
        int y,
        float curX,
        float curY,
        boolean preview)
    {
        ItemStack stack = (channel != null && !channel.isEmpty()) ? channel.interpolate(localTick, ItemStack.EMPTY) : ItemStack.EMPTY;
        boolean isHover = curX >= x && curX <= x + 16 && curY >= y && curY <= y + 16;
        ItemStack shown = drawSlotContents(batcher, stack, x, y, preview, isHover);
        return isHover && shown != null && !shown.isEmpty() ? shown : null;
    }

    public static ItemStack drawSlotContents(
        Batcher2D batcher,
        ItemStack stack,
        int x,
        int y,
        boolean preview,
        boolean hover)
    {
        if (preview)
        {
            drawSlotPreviewHighlight(batcher, x, y);
        }
        if (stack != null && !stack.isEmpty())
        {
            drawSlotItem(batcher, stack, x, y);
        }
        if (hover)
        {
            drawSlotHighlight(batcher, x, y);
        }
        return stack;
    }

    public static Set<String> dragPreviewKeys(GuiPovActionClip clip, String guiId, float tick)
    {
        return GuiSlotDragPreview.decodeKeys(dragPreviewEncoded(clip, guiId, tick));
    }

    public static String dragPreviewEncoded(GuiPovActionClip clip, String guiId, float tick)
    {
        KeyframeChannel<String> channel = clip.getDragSlots(guiId);
        return channel == null || channel.isEmpty() ? "" : channel.interpolate(tick, "");
    }

    public static ItemStack dragPaintItem(
        GuiPovActionClip clip,
        RecordedHudData hudData,
        float globalTick,
        String guiId,
        float localTick,
        String encoded,
        Set<String> dragKeys)
    {
        ItemStack decoded = GuiSlotDragPreview.decodeItem(encoded);
        if (decoded != null && !decoded.isEmpty())
        {
            return decoded;
        }

        KeyframeChannel<ItemStack> cursor = (hudData != null && !hudData.cursorItem.isEmpty())
            ? hudData.cursorItem
            : clip.getCursorItem(guiId);
        float tick = (hudData != null && !hudData.cursorItem.isEmpty()) ? globalTick : localTick;
        ItemStack stack = cursor == null || cursor.isEmpty() ? ItemStack.EMPTY : cursor.interpolate(tick, ItemStack.EMPTY);
        if (stack != null && !stack.isEmpty())
        {
            return stack;
        }

        for (String key : dragKeys)
        {
            if (!key.startsWith("inv_") && !key.startsWith("hotbar_"))
            {
                KeyframeChannel<ItemStack> channel = clip.getGuiSlot(guiId, key);
                ItemStack preview = channel == null || channel.isEmpty()
                    ? ItemStack.EMPTY
                    : channel.interpolate(localTick, ItemStack.EMPTY);
                if (preview != null && !preview.isEmpty())
                {
                    return preview;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void drawSlotPreviewHighlight(Batcher2D batcher, int x, int y)
    {
        batcher.getContext().fill(x, y, x + 16, y + 16, 0x80ffffff);
    }

    public static void drawSlotHighlight(Batcher2D batcher, int x, int y)
    {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, false);
        batcher.getContext().fill(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 16, 0, 0x80ffffff);
        batcher.getContext().draw();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    public static void drawSlotItem(Batcher2D batcher, ItemStack stack, int x, int y)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }
        try
        {
            batcher.getContext().drawItem(stack, x, y);
            batcher.getContext().drawItemInSlot(MinecraftClient.getInstance().textRenderer, stack, x, y);
        }
        catch (Exception ignored)
        {
        }
    }

    public static ItemStack renderDefault(GuiRenderContext ctx)
    {
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        return renderContainerSlots(
            ctx.batcher,
            ctx.replayKeyframes,
            ctx.clip,
            ctx.guiId,
            ctx.localTick,
            ctx.globalTick,
            ctx.opacity,
            cursorX,
            cursorY,
            ctx.hover);
    }

    public static ItemStack renderContainerSlots(
        Batcher2D batcher,
        ReplayKeyframes replayKeyframes,
        GuiPovActionClip clip,
        String guiId,
        float localTick,
        float globalTick,
        float opacity,
        float cursorGuiX,
        float cursorGuiY,
        GuiPointerHover hover)
    {
        ItemStack hovered = null;
        GuiSlotSchema schema = GuiSlotSchema.get(guiId);
        int slotOffsetX = 0;
        int slotOffsetY = 0;
        int invX = 8;
        int invY = 84;

        if ("beacon".equals(guiId))
        {
            invX = 36;
            invY = 137;
        }
        else if ("large_chest".equals(guiId))
        {
            invY = 139;
        }
        else if ("chest".equals(guiId) || "barrel".equals(guiId) || "ender_chest".equals(guiId))
        {
            invY = 85;
        }
        else if ("hopper".equals(guiId))
        {
            invY = 51;
        }
        else if ("villager".equals(guiId))
        {
            invX = 108;
        }
        invX += slotOffsetX;
        invY += slotOffsetY;

        GuiEquipmentRenderer.renderEmptySlotPlaceholders(batcher, clip, guiId, localTick, opacity);
        Set<String> dragKeys = dragPreviewKeys(clip, guiId, localTick);

        RecordedHudData hudData = null;
        if (replayKeyframes instanceof ReplayKeyframesPovAccess access)
        {
            hudData = access.bbsPov$getHud();
        }

        String encoded = dragPreviewEncoded(clip, guiId, localTick);
        ItemStack paint = dragPaintItem(clip, hudData, globalTick, guiId, localTick, encoded, dragKeys);

        if (schema.playerInventory)
        {
            ItemStack playerHovered = GuiPlayerInventoryRenderer.render(
                batcher,
                replayKeyframes,
                hudData,
                clip,
                guiId,
                localTick,
                globalTick,
                cursorGuiX,
                cursorGuiY,
                invX,
                invY,
                dragKeys,
                encoded,
                paint,
                hover);
            if (playerHovered != null && !playerHovered.isEmpty())
            {
                hovered = playerHovered;
            }
        }

        for (GuiSlotSchema.Slot slot : schema.slots)
        {
            if (schema.isChestSlot(slot) && !clip.isMountChestOpen(guiId, localTick))
            {
                continue;
            }

            boolean preview = dragKeys.contains(slot.id());
            KeyframeChannel<ItemStack> channel = clip.getGuiSlot(guiId, slot.id());
            ItemStack stack = (channel != null && !channel.isEmpty()) ? channel.interpolate(localTick, ItemStack.EMPTY) : ItemStack.EMPTY;
            if (preview && paint != null && !paint.isEmpty())
            {
                int existing = stack == null || stack.isEmpty() ? 0 : stack.getCount();
                stack = paint.copyWithCount(GuiSlotDragPreview.previewCount(
                    GuiSlotDragPreview.decodeButton(encoded),
                    GuiSlotDragPreview.decodeOriginalCount(encoded),
                    dragKeys.size(),
                    paint.getMaxCount(),
                    existing));
            }
            int slotX = slot.x() + slotOffsetX;
            int slotY = slot.y() + slotOffsetY;
            boolean isHover = cursorGuiX >= slotX && cursorGuiX <= slotX + 16
                && cursorGuiY >= slotY && cursorGuiY <= slotY + 16;
            ItemStack shown = drawSlotContents(batcher, stack, slotX, slotY, preview, isHover);
            if (isHover && shown != null && !shown.isEmpty())
            {
                hovered = shown;
                hover.itemGroup = false;
            }
        }

        return hovered;
    }
}
