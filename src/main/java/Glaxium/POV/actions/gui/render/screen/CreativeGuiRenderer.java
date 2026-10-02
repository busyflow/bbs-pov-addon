package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.CreativeInventoryTabs;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.recording.GuiSlotDragPreview;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiEquipmentRenderer;
import Glaxium.POV.actions.gui.render.common.GuiItemRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.search.SearchManager;
import net.minecraft.client.search.SearchProvider;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Creative-inventory clip renderer. Saved type id remains {@code creative_inventory}. */
public final class CreativeGuiRenderer implements GuiRenderer, GuiScreenChrome
{
    public static final CreativeGuiRenderer INSTANCE = new CreativeGuiRenderer();

    private static final Identifier CREATIVE_TABS_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

    private CreativeGuiRenderer()
    {
    }

    @Override
    public void render(GuiRenderContext ctx)
    {
        GuiStandardLayoutRenderer.render(ctx, this);
    }

    @Override
    public void drawBackground(GuiRenderContext ctx)
    {
        ItemGroup group = getSelectedGroup(ctx.clip, ctx.localTick);
        int page = MathHelper.clamp(ctx.clip.creativePage.interpolate(ctx.localTick, 0), 0, CreativeInventoryTabs.maxPage());
        drawTabBackgrounds(ctx.batcher, group, page, false);
        ctx.batcher.flush();

        String tabName = "tab_items.png";
        if (group != null)
        {
            if (group.getType() == ItemGroup.Type.INVENTORY)
            {
                tabName = "tab_inventory.png";
            }
            else if (group.getType() == ItemGroup.Type.SEARCH)
            {
                tabName = "tab_item_search.png";
            }
            else
            {
                String raw = group.getTexture();
                if (raw != null && !raw.isEmpty())
                {
                    tabName = raw.startsWith("tab_") ? raw : "tab_" + raw;
                }
            }
        }
        if (!tabName.endsWith(".png"))
        {
            tabName += ".png";
        }
        Identifier texture = new Identifier("minecraft", "textures/gui/container/creative_inventory/" + tabName);
        ctx.batcher.getContext().drawTexture(texture, 0, 0, 0F, 0F, 195, 136, 256, 256);
        ctx.batcher.getContext().draw();

        drawTabBackgrounds(ctx.batcher, group, page, true);
        ctx.batcher.getContext().draw();

        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        drawPaginationButtons(ctx.batcher, ctx.clip, ctx.localTick, page, cursorX, cursorY);
        ctx.batcher.getContext().draw();
    }

    @Override
    public ItemStack renderItems(GuiRenderContext ctx)
    {
        float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000F;
        float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000F;
        return drawInventory(
            ctx.batcher,
            ctx.clip,
            ctx.replayKeyframes,
            ctx.localTick,
            ctx.globalTick,
            cursorX,
            cursorY,
            ctx.hover,
            ctx.cursorHasItem);
    }

    @Override
    public void drawPreview(GuiRenderContext ctx)
    {
        ItemGroup group = getSelectedGroup(ctx.clip, ctx.localTick);
        boolean creativeSurvival = group != null && group.getType() == ItemGroup.Type.INVENTORY;
        if (creativeSurvival)
        {
            GuiEntityPreviewRenderer.drawInventoryPreview(ctx, true);
        }
    }

    public static ItemGroup getSelectedGroup(GuiPovActionClip clip, float localTick)
    {
        GuiItemRenderer.ensureItemGroupsPopulated();
        List<ItemGroup> groups = CreativeInventoryTabs.groups();
        if (groups.isEmpty())
        {
            return null;
        }

        int index = MathHelper.clamp(clip.creativeTab.interpolate(localTick, 0), 0, groups.size() - 1);
        return groups.get(index);
    }

    private static void drawPaginationButtons(
        Batcher2D batcher,
        GuiPovActionClip clip,
        float localTick,
        int page,
        float cursorX,
        float cursorY)
    {
        int maxPage = CreativeInventoryTabs.maxPage();
        if (maxPage <= 0)
        {
            return;
        }

        if (CreativeInventoryTabs.isForge())
        {
            boolean prevHover = GuiTextRenderer.inBounds(cursorX, cursorY, 0, -50, 20, 20);
            int prevV = prevHover ? 86 : 66;
            int prevTextColor = prevHover ? 0xFFFFFFA0 : 0xFFFFFFFF;

            batcher.getContext().drawNineSlicedTexture(WIDGETS_TEXTURE, 0, -50, 20, 20, 20, 4, 200, 20, 0, prevV);
            int prevW = MinecraftClient.getInstance().textRenderer.getWidth("<");
            batcher.getContext().drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal("<"),
                0 + (20 - prevW) / 2,
                -50 + (20 - 8) / 2,
                prevTextColor);

            boolean nextHover = GuiTextRenderer.inBounds(cursorX, cursorY, 175, -50, 20, 20);
            int nextV = nextHover ? 86 : 66;
            int nextTextColor = nextHover ? 0xFFFFFFA0 : 0xFFFFFFFF;

            batcher.getContext().drawNineSlicedTexture(WIDGETS_TEXTURE, 175, -50, 20, 20, 20, 4, 200, 20, 0, nextV);
            int nextW = MinecraftClient.getInstance().textRenderer.getWidth(">");
            batcher.getContext().drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal(">"),
                175 + (20 - nextW) / 2,
                -50 + (20 - 8) / 2,
                nextTextColor);

            String pageStr = (page + 1) + " / " + (maxPage + 1);
            int pageW = MinecraftClient.getInstance().textRenderer.getWidth(pageStr);
            batcher.getContext().drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal(pageStr),
                97 - pageW / 2,
                -44,
                0xFFFFFFFF);

            boolean focusPrev = false;
            boolean focusNext = false;

            if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().currentScreen != null)
            {
                net.minecraft.client.gui.Element focused = MinecraftClient.getInstance().currentScreen.getFocused();
                if (focused instanceof net.minecraft.client.gui.widget.ClickableWidget cw)
                {
                    String msg = cw.getMessage() != null ? cw.getMessage().getString() : "";
                    if ("<".equals(msg))
                    {
                        focusPrev = true;
                    }
                    else if (">".equals(msg))
                    {
                        focusNext = true;
                    }
                }
            }

            if (focusPrev)
            {
                batcher.box(0, -50, 20, -49, 0xFFFFFFFF);
                batcher.box(0, -31, 20, -30, 0xFFFFFFFF);
                batcher.box(0, -49, 1, -31, 0xFFFFFFFF);
                batcher.box(19, -49, 20, -31, 0xFFFFFFFF);
            }
            else if (focusNext)
            {
                batcher.box(175, -50, 195, -49, 0xFFFFFFFF);
                batcher.box(175, -31, 195, -30, 0xFFFFFFFF);
                batcher.box(175, -49, 176, -31, 0xFFFFFFFF);
                batcher.box(194, -49, 195, -31, 0xFFFFFFFF);
            }
        }
        else
        {
            Identifier buttons = new Identifier("fabric", "textures/gui/creative_buttons.png");
            int prevU = (page > 0 && GuiTextRenderer.inBounds(cursorX, cursorY, 170, 4, 11, 12)) ? 22 : 0;
            int prevV = page > 0 ? 0 : 12;
            batcher.getContext().drawTexture(buttons, 170, 4, (float) prevU, (float) prevV, 11, 12, 256, 256);

            int nextU = 11 + ((page < maxPage && GuiTextRenderer.inBounds(cursorX, cursorY, 181, 4, 11, 12)) ? 22 : 0);
            int nextV = page < maxPage ? 0 : 12;
            batcher.getContext().drawTexture(buttons, 181, 4, (float) nextU, (float) nextV, 11, 12, 256, 256);
        }
    }

    private static ItemStack drawInventory(
        Batcher2D batcher,
        GuiPovActionClip clip,
        ReplayKeyframes replayKeyframes,
        float localTick,
        float globalTick,
        float cursorX,
        float cursorY,
        GuiPointerHover hover,
        boolean cursorHasItem)
    {
        ItemGroup selected = getSelectedGroup(clip, localTick);
        if (selected == null)
        {
            return null;
        }

        List<ItemGroup> groups = CreativeInventoryTabs.groups();
        ItemStack hovered = null;
        Text hoverText = null;
        int page = MathHelper.clamp(clip.creativePage.interpolate(localTick, 0), 0, CreativeInventoryTabs.maxPage());

        drawTabIcons(batcher, page);

        for (ItemGroup group : groups)
        {
            if (!CreativeInventoryTabs.visibleOnPage(group, page))
            {
                continue;
            }

            boolean top = CreativeInventoryTabs.isTop(group, page);
            int column = CreativeInventoryTabs.getColumn(group, page);
            int tabX = CreativeInventoryTabs.isSpecial(group, page) ? 195 - 27 * (7 - column) + 1 : 27 * column;

            int hoverY = top ? -32 : 136;
            if (cursorX >= tabX && cursorX <= tabX + 26 && cursorY >= hoverY && cursorY <= hoverY + 32)
            {
                hoverText = group.getDisplayName();
            }
        }

        int maxPage = CreativeInventoryTabs.maxPage();
        if (maxPage > 0 && !CreativeInventoryTabs.isForge())
        {
            if (cursorX >= 170 && cursorX <= 192 && cursorY >= 4 && cursorY <= 16)
            {
                hoverText = Text.translatable("fabric.gui.creativeTabPage", page + 1, maxPage + 1);
            }
        }

        Collection<ItemStack> source = selected.getType() == ItemGroup.Type.SEARCH
            ? selected.getSearchTabStacks()
            : selected.getDisplayStacks();
        List<ItemStack> items = source instanceof List<ItemStack> list ? list : new ArrayList<>(source);
        String search = clip.creativeSearch.isEmpty() ? "" : clip.creativeSearch.interpolate(localTick, "");
        if (search == null)
        {
            search = "";
        }
        String query = search.trim();
        if (!query.isEmpty() && selected.getType() == ItemGroup.Type.SEARCH)
        {
            items = searchItems(query);
        }
        if (selected.getType() == ItemGroup.Type.SEARCH)
        {
            boolean searchHover = GuiTextRenderer.inBounds(cursorX, cursorY, 82, 6, 80, 9);
            boolean focused = GuiTextRenderer.isSearchFocused(clip.creativeSearchFocus, localTick, search, searchHover);
            GuiTextRenderer.drawSearchField(
                batcher,
                search,
                82,
                6,
                80,
                9,
                focused,
                false,
                1F,
                GuiTextRenderer.sampleInt(clip.creativeSearchSelStart, localTick, search.length()),
                GuiTextRenderer.sampleInt(clip.creativeSearchSelEnd, localTick, search.length()));
        }

        RecordedHudData hudData = null;
        if (replayKeyframes instanceof ReplayKeyframesPovAccess access)
        {
            hudData = access.bbsPov$getHud();
        }

        Set<String> dragKeys = GuiSlotRenderer.dragPreviewKeys(clip, "creative_inventory", localTick);
        String encoded = GuiSlotRenderer.dragPreviewEncoded(clip, "creative_inventory", localTick);
        ItemStack paint = GuiSlotRenderer.dragPaintItem(clip, hudData, globalTick, "creative_inventory", localTick, encoded, dragKeys);

        if (selected.getType() != ItemGroup.Type.INVENTORY)
        {
            int rows = Math.max(5, (items.size() + 8) / 9);
            int maxFirstRow = Math.max(0, rows - 5);
            int firstRow = MathHelper.clamp(clip.creativeRow.interpolate(localTick, 0), 0, maxFirstRow);
            int firstItem = firstRow * 9;

            for (int i = 0; i < 45 && firstItem + i < items.size(); i++)
            {
                int x = 9 + (i % 9) * 18;
                int y = 18 + (i / 9) * 18;
                ItemStack stack = items.get(firstItem + i);
                GuiSlotRenderer.drawSlotItem(batcher, stack, x, y);
                if (cursorX >= x && cursorX <= x + 16 && cursorY >= y && cursorY <= y + 16)
                {
                    GuiSlotRenderer.drawSlotHighlight(batcher, x, y);
                    hovered = stack;
                    hover.itemGroup = false;
                }
            }

            if (selected.hasScrollbar())
            {
                float scroll = maxFirstRow == 0 ? 0F : firstRow / (float) maxFirstRow;
                int scrollY = 18 + Math.round(scroll * 95F);
                batcher.getContext().drawTexture(CREATIVE_TABS_TEXTURE, 175, scrollY, 232, 0, 12, 15);
            }
        }
        else
        {
            GuiEquipmentRenderer.renderEmptyEquipmentSlots(batcher, clip, "creative_inventory", localTick);

            for (GuiSlotSchema.Slot slot : GuiSlotSchema.get("creative_inventory").slots)
            {
                ItemStack stack = GuiSlotRenderer.processSlot(
                    batcher,
                    clip.getGuiSlot("creative_inventory", slot.id()),
                    localTick,
                    slot.x(),
                    slot.y(),
                    cursorX,
                    cursorY,
                    dragKeys.contains(slot.id()));
                if (stack != null) {
                    hovered = stack;
                    hover.itemGroup = false;
                }
            }

            for (int i = 0; i < 27; i++)
            {
                int x = 9 + (i % 9) * 18;
                int y = 54 + (i / 9) * 18;
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

                boolean isHover = cursorX >= x && cursorX <= x + 16 && cursorY >= y && cursorY <= y + 16;
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
                ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, x, y, preview, isHover);
                if (isHover && shown != null && !shown.isEmpty())
                {
                    hovered = shown;
                    hover.itemGroup = true;
                }
            }
        }
        for (int i = 0; i < ReplayKeyframes.HOTBAR_SIZE; i++)
        {
            int x = 9 + i * 18;
            int y = 112;
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
            boolean isHover = cursorX >= x && cursorX <= x + 16 && cursorY >= y && cursorY <= y + 16;
            ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, x, y, preview, isHover);
            if (isHover && shown != null && !shown.isEmpty())
            {
                hovered = shown;
                hover.itemGroup = true;
            }
        }

        if (selected.shouldRenderName())
        {
            batcher.text(selected.getDisplayName().getString(), 8, 6, 0xff404040, false);
        }
        if (hoverText != null && !cursorHasItem)
        {
            batcher.flush();
            batcher.getContext().drawTooltip(
                MinecraftClient.getInstance().textRenderer,
                hoverText,
                Math.round(cursorX),
                Math.round(cursorY));
        }
        return hovered;
    }

    private static void drawTabBackgrounds(
        Batcher2D batcher,
        ItemGroup selected,
        int page,
        boolean selectedOnly)
    {
        for (ItemGroup group : CreativeInventoryTabs.groups())
        {
            boolean active = group == selected;
            if (!CreativeInventoryTabs.visibleOnPage(group, page) || active != selectedOnly)
            {
                continue;
            }

            boolean top = CreativeInventoryTabs.isTop(group, page);
            int column = CreativeInventoryTabs.getColumn(group, page);
            int tabX = CreativeInventoryTabs.isSpecial(group, page) ? 195 - 27 * (7 - column) + 1 : 27 * column;
            int tabY = top ? -28 : 132;
            // 1.20.1 tabs are 26 pixels wide, with 27-pixel placement spacing.
            int u = column * 26;
            int v = (top ? 0 : 64) + (active ? 32 : 0);
            batcher.getContext().drawTexture(CREATIVE_TABS_TEXTURE, tabX, tabY, u, v, 26, 32);
        }
    }

    private static void drawTabIcons(Batcher2D batcher, int page)
    {
        DiffuseLighting.enableGuiDepthLighting();

        for (ItemGroup group : CreativeInventoryTabs.groups())
        {
            if (!CreativeInventoryTabs.visibleOnPage(group, page))
            {
                continue;
            }

            boolean top = CreativeInventoryTabs.isTop(group, page);
            int column = CreativeInventoryTabs.getColumn(group, page);
            int tabX = CreativeInventoryTabs.isSpecial(group, page) ? 195 - 27 * (7 - column) + 1 : 27 * column;
            int tabY = top ? -28 : 132;
            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(0F, 0F, 100F);
            batcher.getContext().drawItem(group.getIcon(), tabX + 5, tabY + 8 + (top ? 1 : -1));
            batcher.getContext().getMatrices().pop();
        }
        batcher.getContext().draw();
        DiffuseLighting.disableGuiDepthLighting();
    }



    private static String cachedSearchQuery = null;
    private static List<ItemStack> cachedSearchResults = List.of();

    private static List<ItemStack> searchItems(String query)
    {
        if (query != null && query.equals(cachedSearchQuery))
        {
            return cachedSearchResults;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String lowered = (query == null ? "" : query).toLowerCase(Locale.ROOT);
        List<ItemStack> results = null;
        try
        {
            if (lowered.startsWith("#"))
            {
                SearchProvider<ItemStack> tags = client.getSearchProvider(SearchManager.ITEM_TAG);
                results = tags == null ? List.of() : new ArrayList<>(tags.findAll(lowered.substring(1)));
            }
            else
            {
                SearchProvider<ItemStack> tooltips = client.getSearchProvider(SearchManager.ITEM_TOOLTIP);
                if (tooltips != null)
                {
                    results = new ArrayList<>(tooltips.findAll(lowered));
                }
            }
        }
        catch (Exception ignored)
        {
        }

        if (results == null)
        {
            List<ItemStack> fallback = new ArrayList<>();
            GuiItemRenderer.ensureItemGroupsPopulated();
            ItemGroup searchTab = ItemGroups.getSearchGroup();
            Collection<ItemStack> source = searchTab == null ? List.of() : searchTab.getSearchTabStacks();
            for (ItemStack stack : source)
            {
                if (stack == null || stack.isEmpty())
                {
                    continue;
                }
                if (itemMatchesQuery(stack, lowered))
                {
                    fallback.add(stack);
                }
            }
            results = fallback;
        }

        cachedSearchQuery = query;
        cachedSearchResults = results;
        return results;
    }

    private static boolean itemMatchesQuery(ItemStack stack, String query)
    {
        if (stack.getName().getString().toLowerCase(Locale.ROOT).contains(query))
        {
            return true;
        }
        if (stack.getItem().toString().toLowerCase(Locale.ROOT).contains(query))
        {
            return true;
        }
        try
        {
            for (Text line : Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack))
            {
                if (line.getString().toLowerCase(Locale.ROOT).contains(query))
                {
                    return true;
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return false;
    }



}
