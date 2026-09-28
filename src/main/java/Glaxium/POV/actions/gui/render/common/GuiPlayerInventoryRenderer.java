package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.recording.GuiSlotDragPreview;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.hud.RecordedHudData;
import java.util.Set;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class GuiPlayerInventoryRenderer {
   private GuiPlayerInventoryRenderer() {
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
      GuiPointerHover hover
   ) {
      ItemStack hovered = null;
      boolean playerItemGroup = false;

      for (int i = 0; i < 27; i++) {
         int sx = invX + i % 9 * 18;
         int sy = invY + i / 9 * 18;
         boolean isHover = cursorGuiX >= (float)sx && cursorGuiX <= (float)(sx + 16) && cursorGuiY >= (float)sy && cursorGuiY <= (float)(sy + 16);
         ItemStack stack = ItemStack.EMPTY;
         if (hudData != null && i < hudData.inventory.size()) {
            KeyframeChannel<ItemStack> ch = hudData.inventory.get(i);
            stack = ch != null && !ch.isEmpty() ? (ItemStack)ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
         } else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null) {
            stack = (ItemStack)MinecraftClient.getInstance().player.getInventory().main.get(9 + i);
         }

         boolean preview = dragKeys.contains("inv_" + i);
         if (preview && paint != null && !paint.isEmpty()) {
            int existing = stack != null && !stack.isEmpty() ? stack.getCount() : 0;
            stack = paint.copyWithCount(
               GuiSlotDragPreview.previewCount(
                  GuiSlotDragPreview.decodeButton(encoded), GuiSlotDragPreview.decodeOriginalCount(encoded), dragKeys.size(), paint.getMaxCount(), existing
               )
            );
         }

         ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, sx, sy, preview, isHover);
         if (isHover && shown != null && !shown.isEmpty()) {
            hovered = shown;
            hover.itemGroup = playerItemGroup;
         }
      }

      int hotbarY = invY + 58;

      for (int i = 0; i < 9; i++) {
         int sxx = invX + i * 18;
         boolean isHoverx = cursorGuiX >= (float)sxx && cursorGuiX <= (float)(sxx + 16) && cursorGuiY >= (float)hotbarY && cursorGuiY <= (float)(hotbarY + 16);
         ItemStack stackx = ItemStack.EMPTY;
         if (replayKeyframes != null) {
            KeyframeChannel<ItemStack> ch = (KeyframeChannel<ItemStack>)replayKeyframes.hotbar.get(i);
            stackx = ch != null && !ch.isEmpty() ? (ItemStack)ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
         } else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null) {
            stackx = MinecraftClient.getInstance().player.getInventory().getStack(i);
         }

         boolean previewx = dragKeys.contains("hotbar_" + i);
         if (previewx && paint != null && !paint.isEmpty()) {
            int existing = stackx != null && !stackx.isEmpty() ? stackx.getCount() : 0;
            stackx = paint.copyWithCount(
               GuiSlotDragPreview.previewCount(
                  GuiSlotDragPreview.decodeButton(encoded), GuiSlotDragPreview.decodeOriginalCount(encoded), dragKeys.size(), paint.getMaxCount(), existing
               )
            );
         }

         ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stackx, sxx, hotbarY, previewx, isHoverx);
         if (isHoverx && shown != null && !shown.isEmpty()) {
            hovered = shown;
            hover.itemGroup = playerItemGroup;
         }
      }

      return hovered;
   }
}
