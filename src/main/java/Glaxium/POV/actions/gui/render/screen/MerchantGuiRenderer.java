package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.data.MerchantSnapshot;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Merchant / villager trade screen clip renderer. Saved type id remains {@code villager}. */
public final class MerchantGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final MerchantGuiRenderer INSTANCE = new MerchantGuiRenderer();

    private static final Identifier VILLAGER_TEXTURE = new Identifier("textures/gui/container/villager2.png");
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

    private MerchantGuiRenderer()
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
        drawOffers(ctx.batcher, ctx.clip, ctx.guiId, ctx.localTick, cursorX, cursorY, ctx.hover);
    }

    private static void drawOffers(
        Batcher2D batcher,
        GuiPovActionClip clip,
        String guiId,
        float tick,
        float cursorX,
        float cursorY,
        GuiPointerHover pointerHover)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer textRenderer = mc.textRenderer;

        // 1. Draw "Trades" Title
        Text tradesText = Text.translatable("merchant.trades");
        int tradesWidth = textRenderer.getWidth(tradesText);
        batcher.text(tradesText.getString(), 49 - tradesWidth / 2, 6, 0x404040, false);

        // 2. Villager Level & Profession / Title
        KeyframeChannel<String> titleChan = clip.getMerchantTitle(guiId);
        String customTitle = (titleChan != null && !titleChan.isEmpty()) ? titleChan.interpolate(tick, "") : "";

        KeyframeChannel<Integer> profChan = clip.getMerchantProfession(guiId);
        int profession = (profChan != null && !profChan.isEmpty()) ? profChan.interpolate(tick, 1) : 1;

        KeyframeChannel<Integer> lvlChan = clip.getMerchantLevel(guiId);
        int level = (lvlChan != null && !lvlChan.isEmpty()) ? lvlChan.interpolate(tick, 1) : 1;

        KeyframeChannel<Boolean> canLvlChan = clip.getMerchantCanLevel(guiId);
        boolean canLevel = (canLvlChan == null || canLvlChan.isEmpty()) || canLvlChan.interpolate(tick, true);

        Text displayTitleText = MerchantSnapshot.getTitleText(customTitle, profession, level, canLevel || level >= 1);
        int titleWidth = textRenderer.getWidth(displayTitleText);
        int titleX = 136 + (102 - titleWidth) / 2;
        batcher.text(displayTitleText.getString(), titleX, 6, 0x404040, false);

        // 3. Experience Bar (Disappears when villager becomes Master (level >= 5) or when disabled)
        if (canLevel && level < 5)
        {
            batcher.flush();
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, 136, 16, 0, 186, 102, 5, 512, 256);

            KeyframeChannel<Integer> xpChan = clip.getMerchantExperience(guiId);
            int xp = (xpChan != null && !xpChan.isEmpty()) ? xpChan.interpolate(tick, 0) : 0;

            int minXp = 0;
            int maxXp = 10;
            if (level == 2) { minXp = 10; maxXp = 70; }
            else if (level == 3) { minXp = 70; maxXp = 150; }
            else if (level == 4) { minXp = 150; maxXp = 250; }

            float progress = MathHelper.clamp((float) (xp - minXp) / (float) (maxXp - minXp), 0.0F, 1.0F);
            int fillWidth = Math.round(102 * progress);
            if (fillWidth > 0)
            {
                batcher.getContext().drawTexture(VILLAGER_TEXTURE, 136, 16, 0, 191, fillWidth, 5, 512, 256);
            }
        }

        // 4. Trade Offers List
        KeyframeChannel<String> offersChan = clip.getMerchantOffers(guiId);
        String offersStr = (offersChan != null && !offersChan.isEmpty()) ? offersChan.interpolate(tick, "") : "";
        List<MerchantSnapshot.ParsedOffer> offers = MerchantSnapshot.parse(offersStr);

        if (offers.isEmpty())
        {
            offers = getDefaultOffers(profession);
        }

        KeyframeChannel<Integer> scrollChan = clip.getMerchantScrollOffset(guiId);
        int scrollOffset = (scrollChan != null && !scrollChan.isEmpty()) ? scrollChan.interpolate(tick, 0) : 0;
        int maxScroll = Math.max(0, offers.size() - 7);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        KeyframeChannel<Integer> selChan = clip.getMerchantSelectedOffer(guiId);
        int selectedIndex = (selChan != null && !selChan.isEmpty()) ? selChan.interpolate(tick, 0) : 0;

        for (int i = 0; i < 7; i++)
        {
            int offerIdx = scrollOffset + i;
            if (offerIdx >= offers.size()) break;

            MerchantSnapshot.ParsedOffer offer = offers.get(offerIdx);
            int rowX = 5;
            int rowY = 16 + i * 20;

            boolean isRowHover = cursorX >= rowX && cursorX <= rowX + 88 && cursorY >= rowY && cursorY <= rowY + 20;
            boolean isSelected = offerIdx == selectedIndex;

            // Draw Vanilla Button Box
            batcher.getContext().drawNineSlicedTexture(WIDGETS_TEXTURE, rowX, rowY, 88, 20, 20, 4, 200, 20, 0, isRowHover ? 86 : 66);

            // Draw 1px White Outline for selected trade
            if (isSelected)
            {
                batcher.box(rowX, rowY, rowX + 88, rowY + 1, 0xFFFFFFFF);
                batcher.box(rowX, rowY + 19, rowX + 88, rowY + 20, 0xFFFFFFFF);
                batcher.box(rowX, rowY, rowX + 1, rowY + 20, 0xFFFFFFFF);
                batcher.box(rowX + 87, rowY, rowX + 88, rowY + 20, 0xFFFFFFFF);
            }

            // Vanilla 1.20.1 trade row: buy1 +5,+1, buy2 +40,+1, arrow +60,+3, sell +68,+1.
            if (offer.disabled())
            {
                batcher.getContext().drawTexture(VILLAGER_TEXTURE, rowX + 60, rowY + 3, 25, 171, 10, 9, 512, 256);
            }
            else
            {
                batcher.getContext().drawTexture(VILLAGER_TEXTURE, rowX + 60, rowY + 3, 15, 171, 10, 9, 512, 256);
            }

            // Buy Item 1
            ItemStack buy1 = offer.buy1();
            if (buy1 != null && !buy1.isEmpty())
            {
                GuiSlotRenderer.drawSlotItem(batcher, buy1, rowX + 5, rowY + 1);
                boolean isBuy1Hover = cursorX >= rowX + 5 && cursorX <= rowX + 21 && cursorY >= rowY + 1 && cursorY <= rowY + 17;
                if (isBuy1Hover)
                {
                    pointerHover.item = buy1;
                    pointerHover.itemGroup = false;
                }
                if (offer.specialPrice() < 0)
                {
                    int baseCount = buy1.getCount();
                    int finalCount = Math.max(1, baseCount + offer.specialPrice());
                    String baseText = String.valueOf(baseCount);
                    String finalText = String.valueOf(finalCount);
                    int baseWidth = textRenderer.getWidth(baseText);

                    // Draw red strikethrough directly over the original count digits
                    int countX = rowX + 5 + 16 - baseWidth;
                    int countY = rowY + 1 + 9;
                    batcher.getContext().fill(countX - 1, countY + 4, countX - 1 + baseWidth + 2, countY + 6, 0xFFFF0000);
                }
            }

            // Buy Item 2
            ItemStack buy2 = offer.buy2();
            if (buy2 != null && !buy2.isEmpty())
            {
                GuiSlotRenderer.drawSlotItem(batcher, buy2, rowX + 40, rowY + 1);
                boolean isBuy2Hover = cursorX >= rowX + 40 && cursorX <= rowX + 56 && cursorY >= rowY + 1 && cursorY <= rowY + 17;
                if (isBuy2Hover)
                {
                    pointerHover.item = buy2;
                    pointerHover.itemGroup = false;
                }
            }

            // Sell Item
            ItemStack sell = offer.sell();
            if (sell != null && !sell.isEmpty())
            {
                GuiSlotRenderer.drawSlotItem(batcher, sell, rowX + 68, rowY + 1);
                boolean isSellHover = cursorX >= rowX + 68 && cursorX <= rowX + 84 && cursorY >= rowY + 1 && cursorY <= rowY + 17;
                if (isSellHover)
                {
                    pointerHover.item = sell;
                    pointerHover.itemGroup = false;
                }
            }
        }

        // 5. Scrollbar — vanilla uses u=0/6, v=199 at y=18, not the out-of-stock overlay at 212,0.
        if (offers.size() <= 7)
        {
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, 94, 18, 6, 199, 6, 27, 512, 256);
        }
        else
        {
            int thumbY = 18 + Math.round((139 - 27) * ((float) scrollOffset / (float) maxScroll));
            batcher.getContext().drawTexture(VILLAGER_TEXTURE, 94, thumbY, 0, 199, 6, 27, 512, 256);
        }
    }

    private static List<MerchantSnapshot.ParsedOffer> getDefaultOffers(int profession)
    {
        List<MerchantSnapshot.ParsedOffer> list = new ArrayList<>();
        switch (profession)
        {
            case 1: // Armorer
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 5), ItemStack.EMPTY, new ItemStack(Items.IRON_HELMET, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY, new ItemStack(Items.IRON_BOOTS, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY, new ItemStack(Items.IRON_LEGGINGS, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 9), ItemStack.EMPTY, new ItemStack(Items.IRON_CHESTPLATE, 1), 0, 12, 0, false));
                break;
            case 2: // Butcher
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CHICKEN, 14), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.COOKED_CHICKEN, 5), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PORKCHOP, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.COOKED_PORKCHOP, 5), 0, 16, 0, false));
                break;
            case 3: // Cartographer
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PAPER, 24), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), new ItemStack(Items.COMPASS, 1), new ItemStack(Items.MAP, 1), 0, 12, 0, false));
                break;
            case 4: // Cleric
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.ROTTEN_FLESH, 32), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.REDSTONE, 2), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.LAPIS_LAZULI, 1), 0, 12, 0, false));
                break;
            case 5: // Farmer
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.WHEAT, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.POTATO, 26), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CARROT, 22), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.BREAD, 6), 0, 16, 0, false));
                break;
            case 6: // Fisherman
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COD, 20), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COD, 1), new ItemStack(Items.COOKED_COD, 1), 0, 16, 0, false));
                break;
            case 7: // Fletcher
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.STICK, 32), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.ARROW, 16), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), new ItemStack(Items.GRAVEL, 10), new ItemStack(Items.FLINT, 10), 0, 12, 0, false));
                break;
            case 8: // Leatherworker
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.LEATHER, 6), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 7), ItemStack.EMPTY, new ItemStack(Items.LEATHER_LEGGINGS, 1), 0, 12, 0, false));
                break;
            case 9: // Librarian
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.PAPER, 24), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 9), new ItemStack(Items.BOOK, 1), new ItemStack(Items.ENCHANTED_BOOK, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 9), ItemStack.EMPTY, new ItemStack(Items.BOOKSHELF, 1), 0, 12, 0, false));
                break;
            case 10: // Mason
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.CLAY_BALL, 10), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.BRICK, 10), 0, 16, 0, false));
                break;
            case 12: // Shepherd
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.WHITE_WOOL, 18), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, new ItemStack(Items.SHEARS, 1), 0, 12, 0, false));
                break;
            case 13: // Toolsmith
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_AXE, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_PICKAXE, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 1), ItemStack.EMPTY, new ItemStack(Items.STONE_SHOVEL, 1), 0, 12, 0, false));
                break;
            case 14: // Weaponsmith
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 3), ItemStack.EMPTY, new ItemStack(Items.IRON_AXE, 1), 0, 12, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 2), ItemStack.EMPTY, new ItemStack(Items.IRON_SWORD, 1), 0, 12, 0, false));
                break;
            default: // None / Nitwit / Fallback
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.COAL, 15), ItemStack.EMPTY, new ItemStack(Items.EMERALD, 1), 0, 16, 0, false));
                list.add(new MerchantSnapshot.ParsedOffer(new ItemStack(Items.EMERALD, 4), ItemStack.EMPTY, new ItemStack(Items.IRON_BOOTS, 1), 0, 12, 0, false));
                break;
        }
        return list;
    }

}
