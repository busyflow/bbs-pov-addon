package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.recording.GuiSlotDragPreview;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.hud.RecordedHudData;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.util.Set;

/** Player inventory + hotbar slots drawn on container GUIs. */
public final class GuiPlayerInventoryRenderer
{
    private GuiPlayerInventoryRenderer()
    {
    }

    public static ItemStack render(
        Batcher2D batcher,
        ReplayKeyframes replayKeyframes,
        RecordedHudData hudData,
        GuiPovActionClip clip,
        String guiId,
        float localTick,
        float globalTick,
        float cursorGuiX,
        float cursorGuiY,
        int invX,
        int invY,
        Set<String> dragKeys,
        String encoded,
        ItemStack paint,
        GuiPointerHover hover)
    {
        ItemStack hovered = null;
        boolean playerItemGroup = false;

        for (int i = 0; i < 27; i++)
        {
            int sx = invX + (i % 9) * 18;
            int sy = invY + (i / 9) * 18;
            boolean isHover = cursorGuiX >= sx && cursorGuiX <= sx + 16 && cursorGuiY >= sy && cursorGuiY <= sy + 16;

            ItemStack stack = ItemStack.EMPTY;
            if (hudData != null && i < hudData.inventory.size())
            {
                KeyframeChannel<ItemStack> ch = hudData.inventory.get(i);
                stack = (ch != null && !ch.isEmpty()) ? ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
            }
            else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null)
            {
                stack = MinecraftClient.getInstance().player.getInventory().main.get(9 + i);
            }

            boolean preview = dragKeys.contains("inv_" + i);
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
            ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, sx, sy, preview, isHover);
            if (isHover && shown != null && !shown.isEmpty())
            {
                hovered = shown;
                hover.itemGroup = playerItemGroup;
            }
        }

        int hotbarY = invY + 58;

        for (int i = 0; i < ReplayKeyframes.HOTBAR_SIZE; i++)
        {
            int sx = invX + i * 18;
            boolean isHover = cursorGuiX >= sx && cursorGuiX <= sx + 16
                && cursorGuiY >= hotbarY && cursorGuiY <= hotbarY + 16;
            ItemStack stack = ItemStack.EMPTY;
            if (replayKeyframes != null)
            {
                KeyframeChannel<ItemStack> ch = replayKeyframes.hotbar.get(i);
                stack = (ch != null && !ch.isEmpty()) ? ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
            }
            else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null)
            {
                stack = MinecraftClient.getInstance().player.getInventory().getStack(i);
            }

            boolean preview = dragKeys.contains("hotbar_" + i);
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
            ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, sx, hotbarY, preview, isHover);
            if (isHover && shown != null && !shown.isEmpty())
            {
                hovered = shown;
                hover.itemGroup = playerItemGroup;
            }
        }

        return hovered;
    }
}
