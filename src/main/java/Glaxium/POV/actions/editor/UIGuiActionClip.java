package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.GuiSlotSchema;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.editor.UIGuiSlotEditor;
import Glaxium.POV.actions.gui.editor.UIGuiTypeOverlayPanel;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;

public class UIGuiActionClip extends UIPovActionClip<GuiPovActionClip>
{
    public UIButton editKeyframes;
    public UIButton guiType;
    public UIKeyframeEditor keyframes;
    public UIElement guiKeyframesSection;
    public UISection guiSettingsSection;
    public UIGuiSlotEditor slotEditor;
    private boolean wasSlotSelected = false;

    public UIGuiActionClip(GuiPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.duration(() -> this.clip.duration.get());

        this.slotEditor = new UIGuiSlotEditor(this.clip, this.editor);

        this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), (button) ->
        {
            this.updateKeyframeSheets();
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            if (this.keyframes.view.getGraph() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
        });

        this.guiType = new UIButton(IKey.constant("GUI Type"), (button) ->
        {
            String currentId = this.clip.state.isEmpty() ? "inventory" : this.clip.state.get(0).getValue();
            UIOverlay.addOverlay(this.getContext(), new UIGuiTypeOverlayPanel((entry) ->
            {
                this.editor.editMultiple(this.clip.state, (channel) ->
                {
                    if (channel.isEmpty())
                    {
                        channel.insert(0F, entry.id);
                    }
                    else
                    {
                        channel.get(0).setValue(entry.id);
                    }
                });
                this.updateGuiTypeButton(entry.id);
                this.updateKeyframeSheets();
            }, currentId), 340, 360);
        });
    }

    private void updateKeyframeSheets()
    {
        String currentId = this.clip.state.isEmpty() ? "inventory" : this.clip.state.get(0).getValue();
        GuiSlotSchema schema = GuiSlotSchema.get(currentId);

        this.keyframes.view.removeAllSheets();

        // 1. GUI Layout & Appearance
        float defaultDarkness = "gamemode_switcher".equals(currentId) ? 0.0F : 1.0F;
        this.keyframes.view.addSheet(new UIKeyframeSheet("gui_layout", IKey.constant("GUI Layout"), 0x4aa3df, this.clip.getLayout(currentId), null).icon(Icons.LAYOUT).seed(Transform::new));
        this.keyframes.view.addSheet(new UIKeyframeSheet("gui_opacity", IKey.constant("GUI Opacity"), 0xffffff, this.clip.getOpacity(currentId), null).icon(Icons.COLOR).seed(() -> 1.0F));
        this.keyframes.view.addSheet(new UIKeyframeSheet("bg_opacity", IKey.constant("Darkness Opacity"), 0x888888, this.clip.getDarknessOpacity(currentId), null).icon(Icons.SHAPES).seed(() -> defaultDarkness));

        // 2. All Slot Keyframes (Primary GUI Slots, Crafting Grid, and Inventory Slots)
        if ("donkey".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "gui_slot_saddle",
                IKey.constant("Saddle"),
                0x55ffff,
                this.clip.getGuiSlot(currentId, "saddle"),
                null).icon(Icons.KEY_CAP));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "gui_slots",
                IKey.constant("Donkey Slots"),
                0x55ffff,
                this.clip.getPrimarySlotAnchor(currentId),
                null).icon(Icons.KEY_CAP).seed(() -> true));
        }
        else if (schema.groupedSlots && !schema.slots.isEmpty())
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "gui_slots",
                IKey.constant(GuiTypeEntry.findById(currentId).name + " Slots"),
                0x55ffff,
                this.clip.getPrimarySlotAnchor(currentId),
                null).icon(Icons.KEY_CAP).seed(() -> true));
        }
        else
        {
            int index = 0;
            for (GuiSlotSchema.Slot slot : schema.slots)
            {
                if (schema.isCraftingSlot(slot))
                {
                    continue;
                }

                this.keyframes.view.addSheet(new UIKeyframeSheet(
                    "gui_slot_" + slot.id(),
                    IKey.constant(slot.label()),
                    index++ % 2 == 0 ? 0x55ffff : 0xffaa55,
                    this.clip.getGuiSlot(currentId, slot.id()),
                    null).icon(Icons.KEY_CAP));
            }
        }

        if ("inventory".equals(currentId) || "crafting_table".equals(currentId))
        {
            KeyframeChannel<Boolean> craftingAnchor = this.clip.getCraftingSlotAnchor(currentId);
            if (craftingAnchor != null)
            {
                this.keyframes.view.addSheet(new UIKeyframeSheet(
                    "crafting_grid",
                    IKey.constant("inventory".equals(currentId) ? "Crafting 2x2" : "Crafting 3x3"),
                    0xffaa55,
                    craftingAnchor,
                    null).icon(Icons.KEY_CAP).seed(() -> true));
            }
        }

        // 4. GUI-Specific Controls & Features
        if (GuiRecipeBook.supports(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_open",
                IKey.constant("Recipe Book"),
                0x55cc88,
                this.clip.getRecipeOpen(currentId),
                null).icon(Icons.LOOKING).seed(() -> false));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_showing",
                IKey.constant("Showing Craftable"),
                0x88dd55,
                this.clip.getRecipeShowing(currentId),
                null).icon(Icons.VISIBLE).seed(() -> false));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_search",
                IKey.constant("Recipe Search"),
                0xffffff,
                this.clip.getRecipeSearch(currentId),
                null).icon(Icons.SEARCH).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_search_focus",
                IKey.constant("Recipe Search Focus"),
                0xdddddd,
                this.clip.getRecipeSearchFocus(currentId),
                null).icon(Icons.POINTER).seed(() -> false));
            if (GuiRecipeBook.hasCategories(currentId))
            {
                this.keyframes.view.addSheet(new UIKeyframeSheet(
                    "recipe_category",
                    IKey.constant("Recipe Category"),
                    0xaa88ff,
                    this.clip.getRecipeCategory(currentId),
                    null).icon(Icons.LIST).seed(() -> 0));
            }
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_page",
                IKey.constant("Recipe Page"),
                0x88bbff,
                this.clip.getRecipePage(currentId),
                null).icon(Icons.CURVES).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_selected",
                IKey.constant("Selected Recipe"),
                0xffcc66,
                this.clip.getRecipeSelected(currentId),
                null).icon(Icons.KEY_CAP).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "recipe_button",
                IKey.constant("Recipe Button"),
                0x5599ff,
                this.clip.getRecipeButton(currentId),
                null).icon(Icons.POINTER).seed(() -> false));
        }

        if (GuiRecipeBook.isFurnace(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "furnace_lit",
                IKey.constant("Furnace Fire"),
                0xff6622,
                this.clip.getFurnaceLit(currentId),
                null).icon(Icons.CURVES).seed(() -> 0F));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "furnace_cook",
                IKey.constant("Cook Arrow"),
                0xffcc66,
                this.clip.getFurnaceCook(currentId),
                null).icon(Icons.CURVES).seed(() -> 0F));
        }

        if ("brewing_stand".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "brew_progress",
                IKey.constant("Brew Arrow"),
                0xffffff,
                this.clip.getBrewProgress(currentId),
                null).icon(Icons.CURVES).seed(() -> 0F));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "brew_fuel",
                IKey.constant("Fuel Bar"),
                0xffcc44,
                this.clip.getBrewFuel(currentId),
                null).icon(Icons.CURVES).seed(() -> 0F));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "brew_bubbles",
                IKey.constant("Bubbles"),
                0x88ddff,
                this.clip.getBrewBubbles(currentId),
                null).icon(Icons.BUBBLE).seed(() -> false));
        }

        if ("anvil".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "anvil_name",
                IKey.constant("Anvil Name"),
                0xffffff,
                this.clip.anvilName,
                null).icon(Icons.SEARCH).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "anvil_name_focus",
                IKey.constant("Anvil Caret"),
                0xdddddd,
                this.clip.anvilNameFocus,
                null).icon(Icons.POINTER).seed(() -> false));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "anvil_error",
                IKey.constant("Anvil Cross"),
                0xff5555,
                this.clip.anvilError,
                null).icon(Icons.CLOSE).seed(() -> false));
        }

        if ("gamemode_switcher".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "gamemode_selection",
                IKey.constant("Gamemode"),
                0x55ffaa,
                this.clip.gamemodeSelection,
                null).icon(Icons.WRENCH).seed(() -> 0));
        }

        if ("beacon".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "beacon_level",
                IKey.constant("Beacon Level"),
                0x55ff88,
                this.clip.beaconLevel,
                null).icon(Icons.CURVES).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "beacon_primary",
                IKey.constant("Primary Power"),
                0x55ffaa,
                this.clip.beaconPrimary,
                null).icon(Icons.KEY_CAP).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "beacon_secondary",
                IKey.constant("Secondary Power"),
                0xff5555,
                this.clip.beaconSecondary,
                null).icon(Icons.KEY_CAP).seed(() -> 0));
        }

        if ("creative_inventory".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet("creative_tab", IKey.constant("Creative Tab"), 0xaa88ff, this.clip.creativeTab, null).icon(Icons.LIST).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet("creative_page", IKey.constant("Creative Page"), 0x9988ff, this.clip.creativePage, null).icon(Icons.LIST).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet("creative_row", IKey.constant("Creative Scroll Row"), 0x88bbff, this.clip.creativeRow, null).icon(Icons.CURVES).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet("creative_search", IKey.constant("Creative Search"), 0xffffff, this.clip.creativeSearch, null).icon(Icons.SEARCH).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet("creative_search_focus", IKey.constant("Creative Search Focus"), 0xdddddd, this.clip.creativeSearchFocus, null).icon(Icons.POINTER).seed(() -> false));
        }
        else if ("loom".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "loom_row",
                IKey.constant("Loom Scroll Row"),
                0x88bbff,
                this.clip.loomRow,
                null).icon(Icons.CURVES).seed(() -> 0));
        }
        else if ("stonecutter".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "stonecutter_row",
                IKey.constant("Stonecutter Scroll Row"),
                0x88bbff,
                this.clip.stonecutterRow,
                null).icon(Icons.CURVES).seed(() -> 0));
        }

        if ("book".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_page",
                IKey.constant("Book Page"),
                0x88bbff,
                this.clip.getBookPage(currentId),
                null).icon(Icons.LIST).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_pages",
                IKey.constant("Book Pages"),
                0xffffff,
                this.clip.getBookPages(currentId),
                null).icon(Icons.SEARCH).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_writable",
                IKey.constant("Writable"),
                0x88cc88,
                this.clip.getBookWritable(currentId),
                null).icon(Icons.LOCKED).seed(() -> false));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_signing",
                IKey.constant("Signing"),
                0xffcc66,
                this.clip.getBookSigning(currentId),
                null).icon(Icons.KEY_CAP).seed(() -> false));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_title",
                IKey.constant("Book Title"),
                0xdddddd,
                this.clip.getBookTitle(currentId),
                null).icon(Icons.SEARCH).seed(() -> ""));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "book_author",
                IKey.constant("Book Author"),
                0xc4a484,
                this.clip.getBookAuthor(currentId),
                null).icon(Icons.PLAYER).seed(() -> ""));
        }

        if ("donkey".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "donkey_chest",
                IKey.constant("Donkey Chest"),
                0xffcc66,
                this.clip.getMountChest(currentId),
                null).icon(Icons.LOCKED).seed(() -> false));
        }
        else if ("horse".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "horse_variant",
                IKey.constant("Horse Variant"),
                0xc4a484,
                this.clip.getHorseVariant(currentId),
                null).icon(Icons.PLAYER).seed(() -> 0));
        }
        else if ("villager".equals(currentId))
        {
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_profession",
                IKey.constant("Profession"),
                0x44aa44,
                this.clip.getMerchantProfession(currentId),
                null).icon(Icons.PLAYER).seed(() -> 1));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_level",
                IKey.constant("Villager Level"),
                0xffcc00,
                this.clip.getMerchantLevel(currentId),
                null).icon(Icons.MORE).seed(() -> 1));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_experience",
                IKey.constant("Villager XP"),
                0x55ff55,
                this.clip.getMerchantExperience(currentId),
                null).icon(Icons.CURVES).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_can_level",
                IKey.constant("Show XP Bar"),
                0x88ff88,
                this.clip.getMerchantCanLevel(currentId),
                null).icon(Icons.VISIBLE).seed(() -> true));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_selected",
                IKey.constant("Selected Trade"),
                0x4488ff,
                this.clip.getMerchantSelectedOffer(currentId),
                null).icon(Icons.POINTER).seed(() -> 0));
            this.keyframes.view.addSheet(new UIKeyframeSheet(
                "merchant_scroll",
                IKey.constant("Trade Scroll"),
                0xaaaaaa,
                this.clip.getMerchantScrollOffset(currentId),
                null).icon(Icons.CURVES).seed(() -> 0));
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.guiKeyframesSection = this.section(
            IKey.constant("GUI Keyframes"),
            this.editKeyframes
        );
        this.panels.add(this.guiKeyframesSection);

        this.guiSettingsSection = this.section(
            IKey.constant("GUI Settings"),
            this.guiType
        );
        this.panels.add(this.guiSettingsSection);

        this.slotEditor.full(this);
        this.slotEditor.setVisible(false);
        this.add(this.slotEditor);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        String currentId = this.clip.state.isEmpty() ? "inventory" : this.clip.state.get(0).getValue();
        this.updateGuiTypeButton(currentId);
        this.updateKeyframeSheets();

        this.wasSlotSelected = false;
        if (this.panels != null) this.panels.setVisible(true);
        if (this.slotEditor != null) this.slotEditor.setVisible(false);

        this.updateSelectedKeyframeView();
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        this.updateSelectedKeyframeView();

        super.render(context);
    }

    private void updateSelectedKeyframeView()
    {
        Keyframe<?> selected = null;
        UIKeyframeSheet selectedSheet = null;

        boolean inKeyframeEditor = this.keyframes != null && this.keyframes.hasParent();

        if (inKeyframeEditor && this.keyframes.view != null)
        {
            if (this.keyframes.view.getGraph() != null)
            {
                selected = this.keyframes.view.getGraph().getSelected();
                if (selected != null)
                {
                    selectedSheet = this.keyframes.view.getGraph().getSheet(selected);
                }
            }
            if (selected == null && this.keyframes.view.getDopeSheet() != null)
            {
                selected = this.keyframes.view.getDopeSheet().getSelected();
                if (selected != null)
                {
                    selectedSheet = this.keyframes.view.getDopeSheet().getSheet(selected);
                }
            }
        }
        else if (!inKeyframeEditor && this.keyframes != null && this.keyframes.view != null)
        {
            if (this.keyframes.view.getGraph() != null && this.keyframes.view.getGraph().getSelected() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
            if (this.keyframes.view.getDopeSheet() != null && this.keyframes.view.getDopeSheet().getSelected() != null)
            {
                this.keyframes.view.getDopeSheet().clearSelection();
            }
        }

        String currentId = this.clip.state.isEmpty() ? "inventory" : this.clip.state.get(0).getValue();
        GuiSlotSchema schema = GuiSlotSchema.get(currentId);
        boolean isLargeSlotKeyframe = false;

        if (selected != null && selectedSheet != null)
        {
            String id = selectedSheet.id != null ? selectedSheet.id : "";
            if ("gui_slots".equals(id) && schema.getGroupedSlots().size() > 9)
            {
                isLargeSlotKeyframe = true;
            }
        }

        if (isLargeSlotKeyframe)
        {
            if (this.lastSelectedKeyframe != selected)
            {
                this.lastSelectedKeyframe = selected;
                this.slotEditor.configure(currentId, UIGuiSlotEditor.Mode.GUI, selected.getTick());
            }
        }
        else
        {
            this.lastSelectedKeyframe = null;
        }

        if (this.wasSlotSelected != isLargeSlotKeyframe)
        {
            this.wasSlotSelected = isLargeSlotKeyframe;
            if (this.panels != null) this.panels.setVisible(!isLargeSlotKeyframe);
            if (this.slotEditor != null) this.slotEditor.setVisible(isLargeSlotKeyframe);
            this.resize();
        }
    }

    private Keyframe<?> lastSelectedKeyframe = null;

    private void updateGuiTypeButton(String id)
    {
        GuiTypeEntry entry = GuiTypeEntry.findById(id);
        String name = entry == null ? (id == null || id.isBlank() ? "None" : id) : entry.name;
        this.guiType.label = IKey.constant("GUI: " + name);
    }
}
