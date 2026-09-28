package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.data.MerchantSnapshot;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class MerchantGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final MerchantGuiRenderer INSTANCE = new MerchantGuiRenderer();
   private static final Identifier VILLAGER_TEXTURE = new Identifier("textures/gui/container/villager2.png");
   private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

   private MerchantGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawEarlyChrome(GuiRenderContext ctx) {
      float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000.0F;
      float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000.0F;
      drawOffers(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, cursorX, cursorY, ctx.hover);
   }

   private static void drawOffers(
      Batcher2D batcher, GuiPovActionClip clip, String guiId, float tick, float cursorX, float cursorY, GuiPointerHover pointerHover
   ) {
      MinecraftClient mc = MinecraftClient.getInstance();
      TextRenderer textRenderer = mc.textRenderer;
      Text tradesText = Text.translatable("merchant.trades");
      int tradesWidth = textRenderer.getWidth(tradesText);
      batcher.text(tradesText.getString(), (float)(49 - tradesWidth / 2), 6.0F, 4210752, false);
      KeyframeChannel<String> titleChan = clip.getMerchantTitle(guiId);
      String customTitle = titleChan != null && !titleChan.isEmpty() ? (String)titleChan.interpolate(tick, "") : "";
      KeyframeChannel<Integer> profChan = clip.getMerchantProfession(guiId);
      int profession = profChan != null && !profChan.isEmpty() ? (Integer)profChan.interpolate(tick, 1) : 1;
      KeyframeChannel<Integer> lvlChan = clip.getMerchantLevel(guiId);
      int level = lvlChan != null && !lvlChan.isEmpty() ? (Integer)lvlChan.interpolate(tick, 1) : 1;
      KeyframeChannel<Boolean> canLvlChan = clip.getMerchantCanLevel(guiId);
      boolean canLevel = canLvlChan == null || canLvlChan.isEmpty() || (Boolean)canLvlChan.interpolate(tick, true);
      String displayTitle = MerchantSnapshot.formatTitle(customTitle, profession, level, canLevel || level >= 1);
      int titleWidth = textRenderer.getWidth(displayTitle);
      int titleX = 136 + (102 - titleWidth) / 2;
      batcher.text(displayTitle, (float)titleX, 6.0F, 4210752, false);
      if (canLevel && level < 5) {
         batcher.flush();
         batcher.getContext().drawTexture(VILLAGER_TEXTURE, 136, 16, 0.0F, 186.0F, 102, 5, 512, 256);
         KeyframeChannel<Integer> xpChan = clip.getMerchantExperience(guiId);
         int xp = xpChan != null && !xpChan.isEmpty() ? (Integer)xpChan.interpolate(tick, 0) : 0;
         int minXp = 0;
         int maxXp = 10;
         if (level == 2) {
            minXp = 10;
            maxXp = 70;
         } else if (level == 3) {
            minXp = 70;
            maxXp = 150;
         } else if (level == 4) {
            minXp = 150;
            maxXp = 250;
         }

         float progress = MathHelper.clamp((float)(xp - minXp) / (float)(maxXp - minXp), 0.0F, 1.0F);
         int fillWidth = Math.round(102.0F * progress);
         if (fillWidth > 0) {
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, 136, 16, 0.0F, 191.0F, fillWidth, 5, 512, 256);
         }
      }

      KeyframeChannel<String> offersChan = clip.getMerchantOffers(guiId);
      String offersStr = offersChan != null && !offersChan.isEmpty() ? (String)offersChan.interpolate(tick, "") : "";
      List<MerchantSnapshot.ParsedOffer> offers = MerchantSnapshot.parse(offersStr);
      if (offers.isEmpty()) {
         offers = getDefaultOffers(profession);
      }

      KeyframeChannel<Integer> scrollChan = clip.getMerchantScrollOffset(guiId);
      int scrollOffset = scrollChan != null && !scrollChan.isEmpty() ? (Integer)scrollChan.interpolate(tick, 0) : 0;
      int maxScroll = Math.max(0, offers.size() - 7);
      scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
      KeyframeChannel<Integer> selChan = clip.getMerchantSelectedOffer(guiId);
      int selectedIndex = selChan != null && !selChan.isEmpty() ? (Integer)selChan.interpolate(tick, 0) : 0;

      for (int i = 0; i < 7; i++) {
         int offerIdx = scrollOffset + i;
         if (offerIdx >= offers.size()) {
            break;
         }

         MerchantSnapshot.ParsedOffer offer = offers.get(offerIdx);
         int rowX = 5;
         int rowY = 16 + i * 20;
         boolean isRowHover = cursorX >= (float)rowX && cursorX <= (float)(rowX + 88) && cursorY >= (float)rowY && cursorY <= (float)(rowY + 20);
         boolean isSelected = offerIdx == selectedIndex;
         batcher.getContext().drawNineSlicedTexture(WIDGETS_TEXTURE, rowX, rowY, 88, 20, 20, 4, 200, 20, 0, isRowHover ? 86 : 66);
         if (isSelected) {
            batcher.box((float)rowX, (float)rowY, (float)(rowX + 88), (float)(rowY + 1), -1);
            batcher.box((float)rowX, (float)(rowY + 19), (float)(rowX + 88), (float)(rowY + 20), -1);
            batcher.box((float)rowX, (float)rowY, (float)(rowX + 1), (float)(rowY + 20), -1);
            batcher.box((float)(rowX + 87), (float)rowY, (float)(rowX + 88), (float)(rowY + 20), -1);
         }

         if (offer.disabled()) {
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, rowX + 60, rowY + 3, 25.0F, 171.0F, 10, 9, 512, 256);
         } else {
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, rowX + 60, rowY + 3, 15.0F, 171.0F, 10, 9, 512, 256);
         }

         ItemStack buy1 = offer.buy1();
         if (buy1 != null && !buy1.isEmpty()) {
            GuiSlotRenderer.drawSlotItem(batcher, buy1, rowX + 5, rowY + 1);
            boolean isBuy1Hover = cursorX >= (float)(rowX + 5)
               && cursorX <= (float)(rowX + 21)
               && cursorY >= (float)(rowY + 1)
               && cursorY <= (float)(rowY + 17);
            if (isBuy1Hover) {
               pointerHover.item = buy1;
               pointerHover.itemGroup = false;
            }

            if (offer.specialPrice() < 0) {
               int baseCount = buy1.getCount();
               int finalCount = Math.max(1, baseCount + offer.specialPrice());
               String baseText = String.valueOf(baseCount);
               String finalText = String.valueOf(finalCount);
               int baseWidth = textRenderer.getWidth(baseText);
               int countX = rowX + 5 + 16 - baseWidth;
               int countY = rowY + 1 + 9;
               batcher.getContext().fill(countX - 1, countY + 4, countX - 1 + baseWidth + 2, countY + 6, -65536);
            }
         }

         ItemStack buy2 = offer.buy2();
         if (buy2 != null && !buy2.isEmpty()) {
            GuiSlotRenderer.drawSlotItem(batcher, buy2, rowX + 40, rowY + 1);
            boolean isBuy2Hover = cursorX >= (float)(rowX + 40)
               && cursorX <= (float)(rowX + 56)
               && cursorY >= (float)(rowY + 1)
               && cursorY <= (float)(rowY + 17);
            if (isBuy2Hover) {
               pointerHover.item = buy2;
               pointerHover.itemGroup = false;
            }
         }

         ItemStack sell = offer.sell();
         if (sell != null && !sell.isEmpty()) {
            GuiSlotRenderer.drawSlotItem(batcher, sell, rowX + 68, rowY + 1);
            boolean isSellHover = cursorX >= (float)(rowX + 68)
               && cursorX <= (float)(rowX + 84)
               && cursorY >= (float)(rowY + 1)
               && cursorY <= (float)(rowY + 17);
            if (isSellHover) {
               pointerHover.item = sell;
               pointerHover.itemGroup = false;
            }
         }
      }

      if (offers.size() <= 7) {
         batcher.getContext().drawTexture(VILLAGER_TEXTURE, 94, 18, 6.0F, 199.0F, 6, 27, 512, 256);
      } else {
         int thumbY = 18 + Math.round(112.0F * ((float)scrollOffset / (float)maxScroll));
         batcher.getContext().drawTexture(VILLAGER_TEXTURE, 94, thumbY, 0.0F, 199.0F, 6, 27, 512, 256);
      }
   }

   private static List<MerchantSnapshot.ParsedOffer> getDefaultOffers(int profession) {
      List<MerchantSnapshot.ParsedOffer> list = new ArrayList<>();
      switch (profession) {
         case 1:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 5), ItemStack.EMPTY, new ItemStack(Items.IRON_HELMET, 1), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY, new ItemStack(Items.IRON_BOOTS, 1), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY, new ItemStack(Items.IRON_LEGGINGS, 1), 0, 12, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 9), ItemStack.EMPTY, new ItemStack(Items.IRON_CHESTPLATE, 1), 0, 12, 0, false)
            );
            break;
         case 2:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CHICKEN, 14), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.COOKED_CHICKEN, 5), 0, 16, 0, false)
            );
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PORKCHOP, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.COOKED_PORKCHOP, 5), 0, 16, 0, false)
            );
            break;
         case 3:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PAPER, 24), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), new ItemStack(Items.COMPASS, 1), new ItemStack(Items.MAP, 1), 0, 12, 0, false)
            );
            break;
         case 4:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.ROTTEN_FLESH, 32), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.REDSTONE, 2), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.LAPIS_LAZULI, 1), 0, 12, 0, false));
            break;
         case 5:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.WHEAT, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.POTATO, 26), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CARROT, 22), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.BREAD, 6), 0, 16, 0, false));
            break;
         case 6:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COD, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(
                  new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COD, 1), new ItemStack(Items.COOKED_COD, 1), 0, 16, 0, false
               )
            );
            break;
         case 7:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.STICK, 32), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.ARROW, 16), 0, 12, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(
                  new ItemStack(Items.EMERALD, 1), new ItemStack(Items.GRAVEL, 10), new ItemStack(Items.FLINT, 10), 0, 12, 0, false
               )
            );
            break;
         case 8:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.LEATHER, 6), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY, new ItemStack(Items.LEATHER_LEGGINGS, 1), 0, 12, 0, false)
            );
            break;
         case 9:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PAPER, 24), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(
               new MerchantSnapshot.ParsedOffer(
                  new ItemStack(Items.EMERALD, 9), new ItemStack(Items.BOOK, 1), new ItemStack(Items.ENCHANTED_BOOK, 1), 0, 12, 0, false
               )
            );
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 9), ItemStack.EMPTY, new ItemStack(Items.BOOKSHELF, 1), 0, 12, 0, false));
            break;
         case 10:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CLAY_BALL, 10), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.BRICK, 10), 0, 16, 0, false));
            break;
         case 11:
         default:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY, new ItemStack(Items.IRON_BOOTS, 1), 0, 12, 0, false));
            break;
         case 12:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.WHITE_WOOL, 18), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, new ItemStack(Items.SHEARS, 1), 0, 12, 0, false));
            break;
         case 13:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_AXE, 1), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_PICKAXE, 1), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_SHOVEL, 1), 0, 12, 0, false));
            break;
         case 14:
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY, new ItemStack(Items.IRON_AXE, 1), 0, 12, 0, false));
            list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, new ItemStack(Items.IRON_SWORD, 1), 0, 12, 0, false));
      }

      return list;
   }
}
