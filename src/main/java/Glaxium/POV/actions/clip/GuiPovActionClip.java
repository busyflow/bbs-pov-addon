package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.clip.GuiClipMigration;
import Glaxium.POV.actions.gui.clip.GuiClipNormalizer;
import Glaxium.POV.actions.gui.clip.GuiSlotChannels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

public final class GuiPovActionClip extends PovActionClip {
   public final KeyframeChannel<String> state = this.channel("state", KeyframeFactories.STRING);
   public final KeyframeChannel<Transform> layout = this.channel("gui_layout", KeyframeFactories.TRANSFORM);
   public final KeyframeChannel<Float> opacity = this.channel("gui_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Float> darknessOpacity = this.channel("bg_opacity", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Transform> cursorLayout = this.channel("cursor_layout", KeyframeFactories.TRANSFORM);
   public final KeyframeChannel<Boolean> cursorVisible = this.channel("cursor_visible", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<ItemStack> cursorItem = this.channel("cursor_item", KeyframeFactories.ITEM_STACK);
   public final KeyframeChannel<Integer> mouseButtons = this.channel("mouse_buttons", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> mouseScroll = this.channel("mouse_scroll", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> creativeTab = this.channel("gui_creative_inventory_tab", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> creativePage = this.channel("gui_creative_inventory_page", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> creativeRow = this.channel("gui_creative_inventory_row", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Float> creativeScroll = this.channel("gui_creative_inventory_scroll", KeyframeFactories.FLOAT);
   public final KeyframeChannel<String> creativeSearch = this.channel("gui_creative_inventory_search", KeyframeFactories.STRING);
   public final KeyframeChannel<Boolean> creativeSearchFocus = this.channel("gui_creative_inventory_search_focus", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> loomRow = this.channel("gui_loom_row", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> stonecutterRow = this.channel("gui_stonecutter_row", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> creativeSearchSelStart = this.channel("gui_creative_inventory_search_sel_start", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> creativeSearchSelEnd = this.channel("gui_creative_inventory_search_sel_end", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> recipeSearchSelStart = this.channel("gui_recipe_search_sel_start", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> recipeSearchSelEnd = this.channel("gui_recipe_search_sel_end", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> recipeOpen = this.channel("gui_recipe_open", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<String> recipeSearch = this.channel("gui_recipe_search", KeyframeFactories.STRING);
   public final KeyframeChannel<Boolean> recipeSearchFocus = this.channel("gui_recipe_search_focus", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Boolean> recipeShowing = this.channel("gui_recipe_showing", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> recipeCategory = this.channel("gui_recipe_category", KeyframeFactories.INTEGER);
   public final KeyframeChannel<String> recipeSelected = this.channel("gui_recipe_selected", KeyframeFactories.STRING);
   public final KeyframeChannel<Integer> recipePage = this.channel("gui_recipe_page", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> recipeButton = this.channel("gui_recipe_button", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<String> anvilName = this.channel("gui_anvil_name", KeyframeFactories.STRING);
   public final KeyframeChannel<Boolean> anvilNameFocus = this.channel("gui_anvil_name_focus", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Integer> anvilNameSelStart = this.channel("gui_anvil_name_sel_start", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> anvilNameSelEnd = this.channel("gui_anvil_name_sel_end", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> anvilError = this.channel("gui_anvil_error", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<String> enchantOffers = this.channel("gui_enchant_offers", KeyframeFactories.STRING);
   public final KeyframeChannel<Integer> enchantSeed = this.channel("gui_enchant_seed", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> enchantPlayerLevel = this.channel("gui_enchant_player_level", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Boolean> enchantCreative = this.channel("gui_enchant_creative", KeyframeFactories.BOOLEAN);
   public final KeyframeChannel<Float> enchantBookOpen = this.channel("gui_enchant_book_open", KeyframeFactories.FLOAT);
   public final KeyframeChannel<Integer> gamemodeSelection = this.channel("gui_gamemode_selection", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> beaconPrimary = this.channel("gui_beacon_primary", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> beaconSecondary = this.channel("gui_beacon_secondary", KeyframeFactories.INTEGER);
   public final KeyframeChannel<Integer> beaconLevel = this.channel("gui_beacon_level", KeyframeFactories.INTEGER);
   public final Map<String, KeyframeChannel<ItemStack>> namedSlots = new HashMap<>();
   public final Map<String, Map<String, KeyframeChannel<ItemStack>>> guiSlots = new HashMap<>();
   public final Map<String, KeyframeChannel<Transform>> guiLayouts = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiOpacities = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiDarknessOpacities = new HashMap<>();
   public final Map<String, KeyframeChannel<Transform>> guiCursorLayouts = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiCursorVisibilities = new HashMap<>();
   public final Map<String, KeyframeChannel<ItemStack>> guiCursorItems = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMouseButtons = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMouseScrolls = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiDragSlots = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiPrimarySlotAnchors = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiCraftingSlotAnchors = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiRecipeOpens = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiRecipeSearches = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiRecipeSearchFocuses = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiRecipeShowings = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiRecipeCategories = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiRecipeSelecteds = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiRecipePages = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiRecipeButtons = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiRecipeSearchSelStarts = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiRecipeSearchSelEnds = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiFurnaceLit = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiFurnaceCook = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiBrewProgress = new HashMap<>();
   public final Map<String, KeyframeChannel<Float>> guiBrewFuel = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiBrewBubbles = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiHorseVariants = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiMountChests = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiBookWritables = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiBookSignings = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiBookPagesIndex = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiBookPages = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiBookTitles = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiBookAuthors = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiBookSelStarts = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiBookSelEnds = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiMerchantOffers = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMerchantProfessions = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMerchantLevels = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMerchantExperiences = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMerchantSelectedOffers = new HashMap<>();
   public final Map<String, KeyframeChannel<Integer>> guiMerchantScrollOffsets = new HashMap<>();
   public final Map<String, KeyframeChannel<String>> guiMerchantTitles = new HashMap<>();
   public final Map<String, KeyframeChannel<Boolean>> guiMerchantCanLevels = new HashMap<>();
   public static final int HORSE_VARIANT_COUNT = 35;
   private final Set<Object> slotKeyframesMovedInBatch = new HashSet<>();
   private float slotKeyframeMoveDelta = Float.NaN;
   private long slotKeyframeMoveTime;

   public GuiPovActionClip() {
      for (int i = 0; i < 9; i++) {
         this.registerNamedSlot("crafting_slot_" + i);
      }

      this.registerNamedSlot("craft_result_slot");

      for (int i = 0; i < 54; i++) {
         this.registerNamedSlot("container_slot_" + i);
      }

      this.registerNamedSlot("anvil_input_0");
      this.registerNamedSlot("anvil_input_1");
      this.registerNamedSlot("anvil_result");
      this.registerNamedSlot("furnace_input");
      this.registerNamedSlot("furnace_fuel");
      this.registerNamedSlot("furnace_result");
      this.registerNamedSlot("enchant_item");
      this.registerNamedSlot("enchant_lapis");
      this.registerNamedSlot("brewing_potion_0");
      this.registerNamedSlot("brewing_potion_1");
      this.registerNamedSlot("brewing_potion_2");
      this.registerNamedSlot("brewing_ingredient");
      this.registerNamedSlot("brewing_fuel");
      this.registerNamedSlot("smithing_template");
      this.registerNamedSlot("smithing_base");
      this.registerNamedSlot("smithing_addition");
      this.registerNamedSlot("smithing_result");
      this.registerNamedSlot("grindstone_top");
      this.registerNamedSlot("grindstone_bottom");
      this.registerNamedSlot("grindstone_result");
      this.registerNamedSlot("stonecutter_input");
      this.registerNamedSlot("stonecutter_result");
      this.registerNamedSlot("cartography_map");
      this.registerNamedSlot("cartography_addition");
      this.registerNamedSlot("cartography_result");
      this.registerNamedSlot("loom_banner");
      this.registerNamedSlot("loom_dye");
      this.registerNamedSlot("loom_pattern");
      this.registerNamedSlot("loom_result");
      this.registerNamedSlot("villager_input_1");
      this.registerNamedSlot("villager_input_2");
      this.registerNamedSlot("villager_result");
      this.registerNamedSlot("horse_saddle");
      this.registerNamedSlot("horse_armor");

      for (int i = 0; i < 15; i++) {
         this.registerNamedSlot("horse_chest_" + i);
      }

      for (GuiSlotSchema schema : GuiSlotSchema.getAll()) {
         String prefix = "gui_" + schema.guiId + "_";
         this.guiLayouts.put(schema.guiId, this.channel(prefix + "layout", KeyframeFactories.TRANSFORM));
         this.guiOpacities.put(schema.guiId, this.channel(prefix + "opacity", KeyframeFactories.FLOAT));
         this.guiDarknessOpacities.put(schema.guiId, this.channel(prefix + "darkness", KeyframeFactories.FLOAT));
         this.guiCursorLayouts.put(schema.guiId, this.channel(prefix + "cursor_layout", KeyframeFactories.TRANSFORM));
         this.guiCursorVisibilities.put(schema.guiId, this.channel(prefix + "cursor_visible", KeyframeFactories.BOOLEAN));
         this.guiCursorItems.put(schema.guiId, this.channel(prefix + "cursor_item", KeyframeFactories.ITEM_STACK));
         this.guiMouseButtons.put(schema.guiId, this.channel(prefix + "mouse_buttons", KeyframeFactories.INTEGER));
         this.guiMouseScrolls.put(schema.guiId, this.channel(prefix + "mouse_scroll", KeyframeFactories.INTEGER));
         this.guiDragSlots.put(schema.guiId, this.channel(prefix + "drag_slots", KeyframeFactories.STRING));
         this.guiPrimarySlotAnchors.put(schema.guiId, this.channel(prefix + "primary_slot_keys", KeyframeFactories.BOOLEAN));
         this.guiCraftingSlotAnchors.put(schema.guiId, this.channel(prefix + "crafting_slot_keys", KeyframeFactories.BOOLEAN));
         if (GuiRecipeBook.supports(schema.guiId)) {
            this.guiRecipeOpens.put(schema.guiId, this.channel(prefix + "recipe_open", KeyframeFactories.BOOLEAN));
            this.guiRecipeSearches.put(schema.guiId, this.channel(prefix + "recipe_search", KeyframeFactories.STRING));
            this.guiRecipeSearchFocuses.put(schema.guiId, this.channel(prefix + "recipe_search_focus", KeyframeFactories.BOOLEAN));
            this.guiRecipeShowings.put(schema.guiId, this.channel(prefix + "recipe_showing", KeyframeFactories.BOOLEAN));
            this.guiRecipePages.put(schema.guiId, this.channel(prefix + "recipe_page", KeyframeFactories.INTEGER));
            this.guiRecipeButtons.put(schema.guiId, this.channel(prefix + "recipe_button", KeyframeFactories.BOOLEAN));
            this.guiRecipeSearchSelStarts.put(schema.guiId, this.channel(prefix + "recipe_search_sel_start", KeyframeFactories.INTEGER));
            this.guiRecipeSearchSelEnds.put(schema.guiId, this.channel(prefix + "recipe_search_sel_end", KeyframeFactories.INTEGER));
            this.guiRecipeSelecteds.put(schema.guiId, this.channel(prefix + "recipe_selected", KeyframeFactories.STRING));
            if (GuiRecipeBook.hasCategories(schema.guiId)) {
               this.guiRecipeCategories.put(schema.guiId, this.channel(prefix + "recipe_category", KeyframeFactories.INTEGER));
            }
         }

         if (GuiRecipeBook.isFurnace(schema.guiId)) {
            this.guiFurnaceLit.put(schema.guiId, this.channel(prefix + "lit_progress", KeyframeFactories.FLOAT));
            this.guiFurnaceCook.put(schema.guiId, this.channel(prefix + "cook_progress", KeyframeFactories.FLOAT));
         }

         if ("brewing_stand".equals(schema.guiId)) {
            this.guiBrewProgress.put(schema.guiId, this.channel(prefix + "brew_progress", KeyframeFactories.FLOAT));
            this.guiBrewFuel.put(schema.guiId, this.channel(prefix + "fuel_progress", KeyframeFactories.FLOAT));
            this.guiBrewBubbles.put(schema.guiId, this.channel(prefix + "bubbles", KeyframeFactories.BOOLEAN));
         }

         if ("horse".equals(schema.guiId)) {
            this.guiHorseVariants.put(schema.guiId, this.channel(prefix + "variant", KeyframeFactories.INTEGER));
         }

         if ("donkey".equals(schema.guiId)) {
            this.guiMountChests.put(schema.guiId, this.channel(prefix + "chest", KeyframeFactories.BOOLEAN));
         }

         if ("book".equals(schema.guiId)) {
            this.guiBookWritables.put(schema.guiId, this.channel(prefix + "writable", KeyframeFactories.BOOLEAN));
            this.guiBookSignings.put(schema.guiId, this.channel(prefix + "signing", KeyframeFactories.BOOLEAN));
            this.guiBookPagesIndex.put(schema.guiId, this.channel(prefix + "page", KeyframeFactories.INTEGER));
            this.guiBookPages.put(schema.guiId, this.channel(prefix + "pages", KeyframeFactories.STRING));
            this.guiBookTitles.put(schema.guiId, this.channel(prefix + "title", KeyframeFactories.STRING));
            this.guiBookAuthors.put(schema.guiId, this.channel(prefix + "author", KeyframeFactories.STRING));
            this.guiBookSelStarts.put(schema.guiId, this.channel(prefix + "sel_start", KeyframeFactories.INTEGER));
            this.guiBookSelEnds.put(schema.guiId, this.channel(prefix + "sel_end", KeyframeFactories.INTEGER));
         }

         if ("villager".equals(schema.guiId)) {
            this.guiMerchantOffers.put(schema.guiId, this.channel(prefix + "offers", KeyframeFactories.STRING));
            this.guiMerchantProfessions.put(schema.guiId, this.channel(prefix + "profession", KeyframeFactories.INTEGER));
            this.guiMerchantLevels.put(schema.guiId, this.channel(prefix + "level", KeyframeFactories.INTEGER));
            this.guiMerchantExperiences.put(schema.guiId, this.channel(prefix + "experience", KeyframeFactories.INTEGER));
            this.guiMerchantSelectedOffers.put(schema.guiId, this.channel(prefix + "selected_offer", KeyframeFactories.INTEGER));
            this.guiMerchantScrollOffsets.put(schema.guiId, this.channel(prefix + "scroll_offset", KeyframeFactories.INTEGER));
            this.guiMerchantTitles.put(schema.guiId, this.channel(prefix + "title", KeyframeFactories.STRING));
            this.guiMerchantCanLevels.put(schema.guiId, this.channel(prefix + "can_level", KeyframeFactories.BOOLEAN));
         }

         Map<String, KeyframeChannel<ItemStack>> slots = new HashMap<>();

         for (GuiSlotSchema.Slot slot : schema.slots) {
            String channelId = "gui_" + schema.guiId + "_" + slot.id();
            slots.put(slot.id(), this.channel(channelId, KeyframeFactories.ITEM_STACK));
         }

         this.guiSlots.put(schema.guiId, slots);
      }
   }

   public KeyframeChannel<ItemStack> registerNamedSlot(String id) {
      KeyframeChannel<ItemStack> ch = this.channel(id, KeyframeFactories.ITEM_STACK);
      this.namedSlots.put(id, ch);
      return ch;
   }

   public KeyframeChannel<ItemStack> getNamedSlot(String id) {
      return this.namedSlots.get(id);
   }

   public KeyframeChannel<ItemStack> getGuiSlot(String guiId, String slotId) {
      guiId = this.resolveGuiId(guiId);
      Map<String, KeyframeChannel<ItemStack>> slots = this.guiSlots.get(guiId);
      return slots == null ? null : slots.get(slotId);
   }

   public List<KeyframeChannel<ItemStack>> getGuiSlots(String guiId) {
      guiId = this.resolveGuiId(guiId);
      GuiSlotSchema schema = GuiSlotSchema.get(guiId);
      List<KeyframeChannel<ItemStack>> result = new ArrayList<>();

      for (GuiSlotSchema.Slot slot : schema.slots) {
         KeyframeChannel<ItemStack> channel = this.getGuiSlot(guiId, slot.id());
         if (channel != null) {
            result.add(channel);
         }
      }

      return result;
   }

   public List<KeyframeChannel<ItemStack>> getGroupedGuiSlots(String guiId) {
      guiId = this.resolveGuiId(guiId);
      GuiSlotSchema schema = GuiSlotSchema.get(guiId);
      List<KeyframeChannel<ItemStack>> result = new ArrayList<>();

      for (GuiSlotSchema.Slot slot : schema.getGroupedSlots()) {
         KeyframeChannel<ItemStack> channel = this.getGuiSlot(guiId, slot.id());
         if (channel != null) {
            result.add(channel);
         }
      }

      return result;
   }

   public List<KeyframeChannel<ItemStack>> getCraftingSlots(String guiId) {
      guiId = this.resolveGuiId(guiId);
      GuiSlotSchema schema = GuiSlotSchema.get(guiId);
      List<KeyframeChannel<ItemStack>> result = new ArrayList<>();

      for (GuiSlotSchema.Slot slot : schema.getCraftingSlots()) {
         KeyframeChannel<ItemStack> channel = this.getGuiSlot(guiId, slot.id());
         if (channel != null) {
            result.add(channel);
         }
      }

      return result;
   }

   public KeyframeChannel<Transform> getLayout(String guiId) {
      return this.guiLayouts.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Float> getOpacity(String guiId) {
      return this.guiOpacities.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Float> getDarknessOpacity(String guiId) {
      return this.guiDarknessOpacities.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Transform> getCursorLayout(String guiId) {
      return this.guiCursorLayouts.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getCursorVisible(String guiId) {
      return this.guiCursorVisibilities.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<ItemStack> getCursorItem(String guiId) {
      return this.guiCursorItems.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMouseButtons(String guiId) {
      return this.guiMouseButtons.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMouseScroll(String guiId) {
      return this.guiMouseScrolls.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getDragSlots(String guiId) {
      return this.guiDragSlots.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getPrimarySlotAnchor(String guiId) {
      return this.guiPrimarySlotAnchors.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getCraftingSlotAnchor(String guiId) {
      return this.guiCraftingSlotAnchors.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getRecipeOpen(String guiId) {
      return this.typedOrLegacy(this.guiRecipeOpens, guiId, this.recipeOpen);
   }

   public KeyframeChannel<String> getRecipeSearch(String guiId) {
      return this.typedOrLegacy(this.guiRecipeSearches, guiId, this.recipeSearch);
   }

   public KeyframeChannel<Boolean> getRecipeSearchFocus(String guiId) {
      return this.typedOrLegacy(this.guiRecipeSearchFocuses, guiId, this.recipeSearchFocus);
   }

   public KeyframeChannel<Boolean> getRecipeShowing(String guiId) {
      return this.typedOrLegacy(this.guiRecipeShowings, guiId, this.recipeShowing);
   }

   public KeyframeChannel<Integer> getRecipeCategory(String guiId) {
      return this.typedOrLegacy(this.guiRecipeCategories, guiId, this.recipeCategory);
   }

   public KeyframeChannel<String> getRecipeSelected(String guiId) {
      return this.typedOrLegacy(this.guiRecipeSelecteds, guiId, this.recipeSelected);
   }

   public KeyframeChannel<Integer> getRecipePage(String guiId) {
      return this.typedOrLegacy(this.guiRecipePages, guiId, this.recipePage);
   }

   public KeyframeChannel<Boolean> getRecipeButton(String guiId) {
      return this.typedOrLegacy(this.guiRecipeButtons, guiId, this.recipeButton);
   }

   public KeyframeChannel<Integer> getRecipeSearchSelStart(String guiId) {
      return this.typedOrLegacy(this.guiRecipeSearchSelStarts, guiId, this.recipeSearchSelStart);
   }

   public KeyframeChannel<Integer> getRecipeSearchSelEnd(String guiId) {
      return this.typedOrLegacy(this.guiRecipeSearchSelEnds, guiId, this.recipeSearchSelEnd);
   }

   public KeyframeChannel<Float> getFurnaceLit(String guiId) {
      return this.guiFurnaceLit.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Float> getFurnaceCook(String guiId) {
      return this.guiFurnaceCook.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Float> getBrewProgress(String guiId) {
      return this.guiBrewProgress.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Float> getBrewFuel(String guiId) {
      return this.guiBrewFuel.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getBrewBubbles(String guiId) {
      return this.guiBrewBubbles.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getHorseVariant(String guiId) {
      return this.guiHorseVariants.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getMountChest(String guiId) {
      return this.guiMountChests.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getBookWritable(String guiId) {
      return this.guiBookWritables.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getBookSigning(String guiId) {
      return this.guiBookSignings.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getBookPage(String guiId) {
      return this.guiBookPagesIndex.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getBookPages(String guiId) {
      return this.guiBookPages.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getBookTitle(String guiId) {
      return this.guiBookTitles.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getBookAuthor(String guiId) {
      return this.guiBookAuthors.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getBookSelStart(String guiId) {
      return this.guiBookSelStarts.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getBookSelEnd(String guiId) {
      return this.guiBookSelEnds.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getMerchantOffers(String guiId) {
      return this.guiMerchantOffers.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMerchantProfession(String guiId) {
      return this.guiMerchantProfessions.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMerchantLevel(String guiId) {
      return this.guiMerchantLevels.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMerchantExperience(String guiId) {
      return this.guiMerchantExperiences.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMerchantSelectedOffer(String guiId) {
      return this.guiMerchantSelectedOffers.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Integer> getMerchantScrollOffset(String guiId) {
      return this.guiMerchantScrollOffsets.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<String> getMerchantTitle(String guiId) {
      return this.guiMerchantTitles.get(this.resolveGuiId(guiId));
   }

   public KeyframeChannel<Boolean> getMerchantCanLevel(String guiId) {
      return this.guiMerchantCanLevels.get(this.resolveGuiId(guiId));
   }

   public boolean isMountChestOpen(String guiId, float tick) {
      KeyframeChannel<Boolean> channel = this.getMountChest(guiId);
      if (channel != null && !channel.isEmpty()) {
         Boolean value = (Boolean)channel.interpolate(tick, false);
         return value != null && value;
      } else {
         return false;
      }
   }

   public int sampleHorseVariant(String guiId, float tick) {
      KeyframeChannel<Integer> channel = this.getHorseVariant(guiId);
      if (channel != null && !channel.isEmpty()) {
         Integer value = (Integer)channel.interpolate(tick, 0);
         return value == null ? 0 : Math.max(0, Math.min(34, value));
      } else {
         return 0;
      }
   }

   public static int packHorseVariant(int compact) {
      int clamped = Math.max(0, Math.min(34, compact));
      return clamped % 7 | clamped / 7 << 8;
   }

   public static int unpackHorseVariant(int colorId, int markingId) {
      int color = Math.max(0, Math.min(6, colorId));
      int marking = Math.max(0, Math.min(4, markingId));
      return marking * 7 + color;
   }

   private <T> KeyframeChannel<T> typedOrLegacy(Map<String, KeyframeChannel<T>> typed, String guiId, KeyframeChannel<T> legacy) {
      KeyframeChannel<T> channel = typed.get(this.resolveGuiId(guiId));
      return channel != null ? channel : legacy;
   }

   public boolean isSlotAnchor(KeyframeChannel<?> anchor) {
      return this.guiPrimarySlotAnchors.containsValue(anchor) || this.guiCraftingSlotAnchors.containsValue(anchor);
   }

   public void moveSlotKeyframes(KeyframeChannel<?> anchor, float fromTick, float toTick) {
      float delta = toTick - fromTick;
      long now = System.nanoTime();
      if (now - this.slotKeyframeMoveTime > 100000000L || Float.isNaN(this.slotKeyframeMoveDelta) || Math.abs(this.slotKeyframeMoveDelta - delta) >= 1.0E-4F) {
         this.slotKeyframesMovedInBatch.clear();
         this.slotKeyframeMoveDelta = delta;
      }

      boolean destinationOccupied = false;

      for (Keyframe<?> keyframe : anchor.getKeyframes()) {
         if (Math.abs(keyframe.getTick() - toTick) < 1.0E-4F && Math.abs(keyframe.getTick() - fromTick) >= 1.0E-4F) {
            destinationOccupied = true;
            break;
         }
      }

      for (KeyframeChannel<ItemStack> channel : this.getSlotsForAnchor(anchor)) {
         for (Keyframe<ItemStack> keyframex : channel.getKeyframes()) {
            if (Math.abs(keyframex.getTick() - fromTick) < 1.0E-4F && this.slotKeyframesMovedInBatch.add(keyframex)) {
               keyframex.setTick(toTick, false);
            }
         }

         channel.sort();
      }

      this.slotKeyframeMoveTime = now;
      if (!destinationOccupied) {
         this.slotKeyframesMovedInBatch.clear();
         this.slotKeyframeMoveDelta = Float.NaN;
      }
   }

   public void removeSlotKeyframes(KeyframeChannel<?> anchor, float tick) {
      for (KeyframeChannel<ItemStack> channel : this.getSlotsForAnchor(anchor)) {
         for (int i = channel.getKeyframes().size() - 1; i >= 0; i--) {
            if (Math.abs(((Keyframe)channel.getKeyframes().get(i)).getTick() - tick) < 1.0E-4F) {
               channel.remove(i);
            }
         }
      }
   }

   private List<KeyframeChannel<ItemStack>> getSlotsForAnchor(KeyframeChannel<?> anchor) {
      for (GuiSlotSchema schema : GuiSlotSchema.getAll()) {
         if (this.guiCraftingSlotAnchors.get(schema.guiId) == anchor) {
            return this.getCraftingSlots(schema.guiId);
         }

         if (this.guiPrimarySlotAnchors.get(schema.guiId) == anchor) {
            return schema.groupedSlots ? this.getGroupedGuiSlots(schema.guiId) : this.getGuiSlots(schema.guiId);
         }
      }

      return List.of();
   }

   public String resolveGuiId(String guiId) {
      return this.guiLayouts.containsKey(guiId) ? guiId : "inventory";
   }

   public List<KeyframeChannel<ItemStack>> getContainerSlotsForGui(String guiTypeId) {
      return GuiSlotChannels.containerSlots(this, guiTypeId);
   }

   @Override
   public PovActionType getActionType() {
      return PovActionType.GUI;
   }

   public void ensureBakingBounds() {
      float start = 0.0F;
      float end = (float)((Integer)this.duration.get()).intValue();

      for (KeyframeChannel<?> channel : this.getChannels()) {
         if (channel != null && !channel.isEmpty() && !isOpacityChannel(channel.getId())) {
            List<? extends Keyframe<?>> keyframes = channel.getKeyframes();
            Keyframe<?> first = (Keyframe<?>)keyframes.get(0);
            Keyframe<?> last = (Keyframe<?>)keyframes.get(keyframes.size() - 1);
            if (first.getTick() > start) {
               insertBound(channel, start, first.getValue());
            }

            if (last.getTick() < end) {
               insertBound(channel, end, last.getValue());
            }
         }
      }
   }

   private static boolean isOpacityChannel(String id) {
      return id != null && (id.endsWith("_opacity") || id.endsWith("_darkness"));
   }

   private static void insertBound(KeyframeChannel channel, float tick, Object value) {
      if (value != null && !channel.has(Math.round(tick))) {
         if (value instanceof ItemStack stack) {
            value = stack.copy();
         } else if (value instanceof Transform transform) {
            value = transform.copy();
         }

         channel.insert(tick, value);
      }
   }

   @Override
   public void normalize() {
      super.normalize();
      GuiClipNormalizer.apply(this);
   }

   public void fromData(BaseType data) {
      super.fromData(data);
      GuiClipMigration.migrate(this, data);
   }

   protected Clip create() {
      return new GuiPovActionClip();
   }
}
