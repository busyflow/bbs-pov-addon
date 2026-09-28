package Glaxium.POV.actions.gui.render.screen;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.CreativeInventoryTabs;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.recording.GuiSlotDragPreview;
import Glaxium.POV.actions.gui.render.GuiPointerHover;
import Glaxium.POV.actions.gui.render.GuiRenderContext;
import Glaxium.POV.actions.gui.render.GuiRenderer;
import Glaxium.POV.actions.gui.render.GuiScreenChrome;
import Glaxium.POV.actions.gui.render.LiveGuiPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiEntityPreviewRenderer;
import Glaxium.POV.actions.gui.render.common.GuiEquipmentRenderer;
import Glaxium.POV.actions.gui.render.common.GuiItemRenderer;
import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.actions.gui.render.common.GuiStandardLayoutRenderer;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import net.minecraft.item.ItemGroup.Row;
import net.minecraft.item.ItemGroup.Type;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class CreativeGuiRenderer implements GuiRenderer, GuiScreenChrome {
   public static final CreativeGuiRenderer INSTANCE = new CreativeGuiRenderer();
   private static final Identifier CREATIVE_TABS_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
   private static String cachedSearchQuery = null;
   private static List<ItemStack> cachedSearchResults = List.of();

   private CreativeGuiRenderer() {
   }

   @Override
   public void render(GuiRenderContext ctx) {
      GuiStandardLayoutRenderer.render(ctx, this);
   }

   @Override
   public void drawBackground(GuiRenderContext ctx) {
      ItemGroup group = getSelectedGroup(ctx.clip, ctx.localTick);
      int page = MathHelper.clamp((Integer)ctx.clip.creativePage.interpolate(ctx.localTick, 0), 0, CreativeInventoryTabs.maxPage());
      drawTabBackgrounds(ctx.batcher, group, page, false);
      ctx.batcher.flush();
      Identifier texture = group == null
         ? ctx.entry.texture
         : new Identifier("minecraft", "textures/gui/container/creative_inventory/tab_" + group.getTexture());
      ctx.batcher.getContext().drawTexture(texture, 0, 0, 0.0F, 0.0F, 195, 136, 256, 256);
   }

   @Override
   public ItemStack renderItems(GuiRenderContext ctx) {
      float cursorX = ctx.cursorVisible ? ctx.cursorGuiX : -1000.0F;
      float cursorY = ctx.cursorVisible ? ctx.cursorGuiY : -1000.0F;
      return drawInventory(ctx.batcher, ctx.clip, ctx.replayKeyframes, ctx.localTick, ctx.globalTick, cursorX, cursorY, ctx.hover, ctx.cursorHasItem);
   }

   @Override
   public void drawPreview(GuiRenderContext ctx) {
      ItemGroup group = getSelectedGroup(ctx.clip, ctx.localTick);
      boolean creativeSurvival = group != null && group.getType() == Type.INVENTORY;
      if (creativeSurvival) {
         GuiEntityPreviewRenderer.drawInventoryPreview(ctx, true);
      }
   }

   public static ItemGroup getSelectedGroup(GuiPovActionClip clip, float localTick) {
      GuiItemRenderer.ensureItemGroupsPopulated();
      List<ItemGroup> groups = CreativeInventoryTabs.groups();
      if (groups.isEmpty()) {
         return null;
      } else {
         int index = MathHelper.clamp((Integer)clip.creativeTab.interpolate(localTick, 0), 0, groups.size() - 1);
         return groups.get(index);
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
      boolean cursorHasItem
   ) {
      ItemGroup selected = getSelectedGroup(clip, localTick);
      if (selected == null) {
         return null;
      } else {
         List<ItemGroup> groups = CreativeInventoryTabs.groups();
         ItemStack hovered = null;
         Text hoverText = null;
         int page = MathHelper.clamp((Integer)clip.creativePage.interpolate(localTick, 0), 0, CreativeInventoryTabs.maxPage());
         drawTabBackgrounds(batcher, selected, page, true);
         drawTabIcons(batcher, page);

         for (ItemGroup group : groups) {
            if (CreativeInventoryTabs.visibleOnPage(group, page)) {
               boolean top = group.getRow() == Row.TOP;
               int column = MathHelper.clamp(group.getColumn(), 0, 6);
               int tabX = group.isSpecial() ? 195 - 27 * (7 - column) + 1 : 27 * column;
               int hoverY = top ? -32 : 136;
               if (cursorX >= (float)tabX && cursorX <= (float)(tabX + 26) && cursorY >= (float)hoverY && cursorY <= (float)(hoverY + 32)) {
                  hoverText = group.getDisplayName();
               }
            }
         }

         int maxPage = CreativeInventoryTabs.maxPage();
         if (maxPage > 0) {
            Identifier buttonsTexture = new Identifier("fabric", "textures/gui/creative_buttons.png");
            boolean previousHover = GuiTextRenderer.inBounds(cursorX, cursorY, 170, 4, 11, 12);
            boolean nextHover = GuiTextRenderer.inBounds(cursorX, cursorY, 181, 4, 11, 12);
            batcher.getContext().drawTexture(buttonsTexture, 170, 4, page > 0 && previousHover ? 22 : 0, page > 0 ? 0 : 12, 11, 12);
            batcher.getContext().drawTexture(buttonsTexture, 181, 4, 11 + (page < maxPage && nextHover ? 22 : 0), page < maxPage ? 0 : 12, 11, 12);
            if (cursorX >= 170.0F && cursorX <= 192.0F && cursorY >= 4.0F && cursorY <= 16.0F) {
               hoverText = Text.literal("Page " + (page + 1) + "/" + (maxPage + 1));
            }
         }

         Collection<ItemStack> source = selected.getType() == Type.SEARCH ? selected.getSearchTabStacks() : selected.getDisplayStacks();
         List<ItemStack> items = (List<ItemStack>)(source instanceof List<ItemStack> list ? list : new ArrayList<>(source));
         String search = clip.creativeSearch.isEmpty() ? "" : (String)clip.creativeSearch.interpolate(localTick, "");
         if (search == null) {
            search = "";
         }

         String query = search.trim();
         if (!query.isEmpty() && selected.getType() == Type.SEARCH) {
            items = searchItems(query);
         }

         if (selected.getType() == Type.SEARCH) {
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
               1.0F,
               GuiTextRenderer.sampleInt(clip.creativeSearchSelStart, localTick, search.length()),
               GuiTextRenderer.sampleInt(clip.creativeSearchSelEnd, localTick, search.length())
            );
         }

         RecordedHudData hudData = null;
         if (replayKeyframes instanceof ReplayKeyframesPovAccess access) {
            hudData = access.bbsPov$getHud();
         }

         Set<String> dragKeys = GuiSlotRenderer.dragPreviewKeys(clip, "creative_inventory", localTick);
         String encoded = GuiSlotRenderer.dragPreviewEncoded(clip, "creative_inventory", localTick);
         ItemStack paint = GuiSlotRenderer.dragPaintItem(clip, hudData, globalTick, "creative_inventory", localTick, encoded, dragKeys);
         if (selected.getType() != Type.INVENTORY) {
            int rows = Math.max(5, (items.size() + 8) / 9);
            int maxFirstRow = Math.max(0, rows - 5);
            int firstRow = MathHelper.clamp((Integer)clip.creativeRow.interpolate(localTick, 0), 0, maxFirstRow);
            int firstItem = firstRow * 9;

            for (int i = 0; i < 45 && firstItem + i < items.size(); i++) {
               int x = 9 + i % 9 * 18;
               int y = 18 + i / 9 * 18;
               ItemStack stack = items.get(firstItem + i);
               GuiSlotRenderer.drawSlotItem(batcher, stack, x, y);
               if (cursorX >= (float)x && cursorX <= (float)(x + 16) && cursorY >= (float)y && cursorY <= (float)(y + 16)) {
                  GuiSlotRenderer.drawSlotHighlight(batcher, x, y);
                  hovered = stack;
                  hover.itemGroup = false;
               }
            }

            if (selected.hasScrollbar()) {
               float scroll = maxFirstRow == 0 ? 0.0F : (float)firstRow / (float)maxFirstRow;
               int scrollY = 18 + Math.round(scroll * 95.0F);
               batcher.getContext().drawTexture(CREATIVE_TABS_TEXTURE, 175, scrollY, 232, 0, 12, 15);
            }
         } else {
            GuiEquipmentRenderer.renderEmptyEquipmentSlots(batcher, clip, "creative_inventory", localTick);

            for (GuiSlotSchema.Slot slot : GuiSlotSchema.get("creative_inventory").slots) {
               ItemStack stack = GuiSlotRenderer.processSlot(
                  batcher, clip.getGuiSlot("creative_inventory", slot.id()), localTick, slot.x(), slot.y(), cursorX, cursorY, dragKeys.contains(slot.id())
               );
               if (stack != null) {
                  hovered = stack;
                  hover.itemGroup = false;
               }
            }

            for (int ix = 0; ix < 27; ix++) {
               int x = 9 + ix % 9 * 18;
               int y = 54 + ix / 9 * 18;
               ItemStack stack = ItemStack.EMPTY;
               if (hudData != null && ix < hudData.inventory.size()) {
                  KeyframeChannel<ItemStack> ch = hudData.inventory.get(ix);
                  stack = ch != null && !ch.isEmpty() ? (ItemStack)ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
               } else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null) {
                  stack = (ItemStack)MinecraftClient.getInstance().player.getInventory().main.get(9 + ix);
               }

               boolean isHover = cursorX >= (float)x && cursorX <= (float)(x + 16) && cursorY >= (float)y && cursorY <= (float)(y + 16);
               boolean preview = dragKeys.contains("inv_" + ix);
               if (preview && paint != null && !paint.isEmpty()) {
                  int existing = stack != null && !stack.isEmpty() ? stack.getCount() : 0;
                  stack = paint.copyWithCount(
                     GuiSlotDragPreview.previewCount(
                        GuiSlotDragPreview.decodeButton(encoded),
                        GuiSlotDragPreview.decodeOriginalCount(encoded),
                        dragKeys.size(),
                        paint.getMaxCount(),
                        existing
                     )
                  );
               }

               ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stack, x, y, preview, isHover);
               if (isHover && shown != null && !shown.isEmpty()) {
                  hovered = shown;
                  hover.itemGroup = true;
               }
            }
         }

         for (int ix = 0; ix < 9; ix++) {
            int xx = 9 + ix * 18;
            int yx = 112;
            ItemStack stackx = ItemStack.EMPTY;
            if (replayKeyframes != null) {
               KeyframeChannel<ItemStack> ch = (KeyframeChannel<ItemStack>)replayKeyframes.hotbar.get(ix);
               stackx = ch != null && !ch.isEmpty() ? (ItemStack)ch.interpolate(globalTick, ItemStack.EMPTY) : ItemStack.EMPTY;
            } else if (LiveGuiPreviewRenderer.isRenderingLive() && MinecraftClient.getInstance().player != null) {
               stackx = MinecraftClient.getInstance().player.getInventory().getStack(ix);
            }

            boolean previewx = dragKeys.contains("hotbar_" + ix);
            if (previewx && paint != null && !paint.isEmpty()) {
               int existing = stackx != null && !stackx.isEmpty() ? stackx.getCount() : 0;
               stackx = paint.copyWithCount(
                  GuiSlotDragPreview.previewCount(
                     GuiSlotDragPreview.decodeButton(encoded), GuiSlotDragPreview.decodeOriginalCount(encoded), dragKeys.size(), paint.getMaxCount(), existing
                  )
               );
            }

            boolean isHoverx = cursorX >= (float)xx && cursorX <= (float)(xx + 16) && cursorY >= (float)yx && cursorY <= (float)(yx + 16);
            ItemStack shown = GuiSlotRenderer.drawSlotContents(batcher, stackx, xx, yx, previewx, isHoverx);
            if (isHoverx && shown != null && !shown.isEmpty()) {
               hovered = shown;
               hover.itemGroup = true;
            }
         }

         if (selected.shouldRenderName()) {
            batcher.text(selected.getDisplayName().getString(), 8.0F, 6.0F, -12566464, false);
         }

         if (hoverText != null && !cursorHasItem) {
            batcher.flush();
            batcher.getContext().drawTooltip(MinecraftClient.getInstance().textRenderer, hoverText, Math.round(cursorX), Math.round(cursorY));
         }

         return hovered;
      }
   }

   private static void drawTabBackgrounds(Batcher2D batcher, ItemGroup selected, int page, boolean selectedOnly) {
      for (ItemGroup group : CreativeInventoryTabs.groups()) {
         boolean active = group == selected;
         if (CreativeInventoryTabs.visibleOnPage(group, page) && active == selectedOnly) {
            boolean top = group.getRow() == Row.TOP;
            int column = MathHelper.clamp(group.getColumn(), 0, 6);
            int tabX = group.isSpecial() ? 195 - 27 * (7 - column) + 1 : 27 * column;
            int tabY = top ? -28 : 132;
            int u = column * 26;
            int v = (top ? 0 : 64) + (active ? 32 : 0);
            batcher.getContext().drawTexture(CREATIVE_TABS_TEXTURE, tabX, tabY, u, v, 26, 32);
         }
      }
   }

   private static void drawTabIcons(Batcher2D batcher, int page) {
      DiffuseLighting.enableGuiDepthLighting();

      for (ItemGroup group : CreativeInventoryTabs.groups()) {
         if (CreativeInventoryTabs.visibleOnPage(group, page)) {
            boolean top = group.getRow() == Row.TOP;
            int column = MathHelper.clamp(group.getColumn(), 0, 6);
            int tabX = group.isSpecial() ? 195 - 27 * (7 - column) + 1 : 27 * column;
            int tabY = top ? -28 : 132;
            batcher.getContext().getMatrices().push();
            batcher.getContext().getMatrices().translate(0.0F, 0.0F, 100.0F);
            batcher.getContext().drawItem(group.getIcon(), tabX + 5, tabY + 8 + (top ? 1 : -1));
            batcher.getContext().getMatrices().pop();
         }
      }
   }

   private static List<ItemStack> searchItems(String query) {
      if (query != null && query.equals(cachedSearchQuery)) {
         return cachedSearchResults;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         String lowered = (query == null ? "" : query).toLowerCase(Locale.ROOT);
         List<ItemStack> results = null;

         try {
            if (lowered.startsWith("#")) {
               SearchProvider<ItemStack> tags = client.getSearchProvider(SearchManager.ITEM_TAG);
               results = (List<ItemStack>)(tags == null ? List.of() : new ArrayList<>(tags.findAll(lowered.substring(1))));
            } else {
               SearchProvider<ItemStack> tooltips = client.getSearchProvider(SearchManager.ITEM_TOOLTIP);
               if (tooltips != null) {
                  results = new ArrayList<>(tooltips.findAll(lowered));
               }
            }
         } catch (Exception var9) {
         }

         if (results == null) {
            List<ItemStack> fallback = new ArrayList<>();
            GuiItemRenderer.ensureItemGroupsPopulated();
            ItemGroup searchTab = ItemGroups.getSearchGroup();

            for (ItemStack stack : searchTab == null ? List.of() : searchTab.getSearchTabStacks()) {
               if (stack != null && !stack.isEmpty() && itemMatchesQuery(stack, lowered)) {
                  fallback.add(stack);
               }
            }

            results = fallback;
         }

         cachedSearchQuery = query;
         cachedSearchResults = results;
         return results;
      }
   }

   private static boolean itemMatchesQuery(ItemStack stack, String query) {
      if (stack.getName().getString().toLowerCase(Locale.ROOT).contains(query)) {
         return true;
      } else if (stack.getItem().toString().toLowerCase(Locale.ROOT).contains(query)) {
         return true;
      } else {
         try {
            for (Text line : Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack)) {
               if (line.getString().toLowerCase(Locale.ROOT).contains(query)) {
                  return true;
               }
            }
         } catch (Exception var4) {
         }

         return false;
      }
   }
}
