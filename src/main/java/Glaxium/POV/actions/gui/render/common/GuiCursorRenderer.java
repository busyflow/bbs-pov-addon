package Glaxium.POV.actions.gui.render.common;

import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroup.Type;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GuiCursorRenderer {
   private GuiCursorRenderer() {
   }

   public static void renderTooltips(GuiRenderContext ctx, ItemStack hoveredStack) {
      if (ctx.cursorVisible && !ctx.cursorHasItem) {
         Batcher2D batcher = ctx.batcher;
         GuiPointerHover pointerHover = ctx.hover;
         float curScreenX = ctx.curScreenX;
         float curScreenY = ctx.curScreenY;
         if (pointerHover.lines != null && !pointerHover.lines.isEmpty()) {
            try {
               batcher.getContext().drawTooltip(MinecraftClient.getInstance().textRenderer, pointerHover.lines, (int)curScreenX, (int)curScreenY);
               batcher.getContext().draw();
            } catch (Exception var11) {
            }
         } else if (pointerHover.widget != null) {
            try {
               batcher.getContext().drawTooltip(MinecraftClient.getInstance().textRenderer, pointerHover.widget, (int)curScreenX, (int)curScreenY);
               batcher.getContext().draw();
            } catch (Exception var10) {
            }
         } else if (hoveredStack != null && !hoveredStack.isEmpty()) {
            try {
               GuiItemRenderer.ensureItemGroupsPopulated();
               List<Text> tooltipLines = new ArrayList<>(Screen.getTooltipFromItem(MinecraftClient.getInstance(), hoveredStack));
               if (pointerHover.itemGroup && !hidesItemGroupLine(hoveredStack)) {
                  int insertIndex = Math.min(1, tooltipLines.size());

                  for (ItemGroup group : ItemGroups.getGroupsToDisplay()) {
                     if (group.getType() != Type.SEARCH && group.getType() != Type.HOTBAR && group.contains(hoveredStack)) {
                        tooltipLines.add(insertIndex, group.getDisplayName().copy().formatted(Formatting.BLUE));
                        break;
                     }
                  }
               }

               batcher.getContext().drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipLines, (int)curScreenX, (int)curScreenY);
               batcher.getContext().draw();
            } catch (Exception var12) {
            }
         }
      }
   }

   private static boolean hidesItemGroupLine(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return true;
      } else if (stack.hasEnchantments()) {
         return true;
      } else {
         NbtCompound nbt = stack.getNbt();
         return nbt != null && nbt.contains("StoredEnchantments", 9) && !nbt.getList("StoredEnchantments", 10).isEmpty();
      }
   }
}
