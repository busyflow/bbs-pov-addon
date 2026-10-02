package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.screeneffect.ScreenEffectEntry;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresetEntry;
import Glaxium.POV.actions.screeneffect.ScreenEffectPresets;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.MenuIcon;
import mchorse.bbs_mod.ui.utils.context.MenuVerb;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIUnifiedPickOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.util.List;
import java.util.function.Consumer;

/**
 * Inspector panel for ScreenEffectPovActionClip.
 * Features right-click context menu to add/remove effects, fixed vanilla depth ordering,
 * and keyframe-driven adjustment via collapsible folder sheets in UIKeyframeEditor.
 */
public class UIScreenEffectActionClip extends UIPovActionClip<ScreenEffectPovActionClip>
{
    public static class ScreenEffectContextAction extends ContextAction
    {
        private final ScreenEffectPresetEntry preset;

        public ScreenEffectContextAction(ScreenEffectPresetEntry preset, Runnable runnable)
        {
            super(Icons.NONE, IKey.constant(preset.name), runnable);
            this.preset = preset;
        }

        @Override
        public int getWidth(FontRenderer font)
        {
            return 32 + font.getWidth(this.label.get());
        }

        @Override
        public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
        {
            this.renderBackground(context, x, y, w, h, hover, selected);

            int iconX = x + 3;
            int iconY = y + (h - 16) / 2;

            context.batcher.box(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xCC000000);

            DrawContext dc = context.batcher.getContext();
            if (dc != null)
            {
                ItemStack stack = this.preset.createIconStack();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                RenderSystem.depthMask(true);
                dc.drawItem(stack, iconX, iconY);
                RenderSystem.disableDepthTest();
            }

            context.batcher.text(this.label.get(), x + 24, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);
        }
    }

    public static class UIScreenEffectList extends UIList<ScreenEffectEntry>
    {
        private final UIScreenEffectActionClip parent;

        public UIScreenEffectList(UIScreenEffectActionClip parent, Consumer<List<ScreenEffectEntry>> callback)
        {
            super(callback);
            this.parent = parent;
            this.scroll.scrollItemSize = 22;
            this.emptyState(IKey.constant("Right-click to add screen effects"));
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (context.mouseButton == 1 && this.area.isInside(context))
            {
                this.parent.openAddEffectMenu(context);
                return true;
            }
            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xCC141414);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0x33FFFFFF);
            super.render(context);
        }

        @Override
        public void renderListElement(UIContext context, ScreenEffectEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            RowStyle.row(context.batcher, x, y, this.area.w, h, this.rowColor(element), this.isHeader(element), hover, selected);
            this.renderElementPart(context, element, i, x, y, hover, selected);
        }

        @Override
        protected void renderElementPart(UIContext context, ScreenEffectEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            int iconX = x + 3;
            int iconY = y + (h - 16) / 2;

            context.batcher.box(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xCC000000);

            DrawContext dc = context.batcher.getContext();
            if (dc != null)
            {
                ItemStack stack = element.createIconStack();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(515);
                RenderSystem.depthMask(true);
                dc.drawItem(stack, iconX, iconY);
                RenderSystem.disableDepthTest();
            }

            int textX = x + 24;
            String text = element.getDisplayName();
            context.batcher.textShadow(text, textX, y + (h - context.batcher.getFont().getHeight()) / 2, RowStyle.textColor(hover || selected));
        }
    }

    public static ScreenEffectPovActionClip currentClip;
    public static UIScreenEffectActionClip currentPanel;

    public UIScreenEffectList effectsList;
    public UIButton editKeyframes;
    public UIKeyframeEditor keyframes;
    public UIButton pickTotemItem;
    public UIIcon clearTotemItem;
    public UIElement totemSection;
    public UIColor effectColor;
    public UIElement colorSection;

    private ScreenEffectEntry selected;

    public UIScreenEffectActionClip(ScreenEffectPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
        currentClip = clip;
        currentPanel = this;
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();
        currentClip = this.clip;
        currentPanel = this;

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.full(this.keyframes).w(1F);
        this.keyframes.view.duration(() -> this.clip.duration.get());

        this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), (button) ->
        {
            currentClip = this.clip;
            currentPanel = this;
            this.updateKeyframeSheets();
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            if (this.keyframes.view.getGraph() != null)
            {
                this.keyframes.view.getGraph().clearSelection();
            }
        });

        this.pickTotemItem = new UIButton(IKey.constant("Pick Item"), (b) -> this.openTotemItemPicker());
        this.pickTotemItem.tooltip(IKey.constant("Select item to display during totem popup"));
        this.pickTotemItem.h(20);

        this.clearTotemItem = new UIIcon(Icons.CLOSE, (b) ->
        {
            this.clip.totemItem.set("minecraft:totem_of_undying");
            this.fillData();
            this.editor.fillData();
        });
        this.clearTotemItem.tooltip(IKey.constant("Reset to default Totem of Undying"));
        this.clearTotemItem.wh(20, 20);

        this.totemSection = UI.column(
            UI.label(IKey.constant("Totem Item")),
            UI.row(this.pickTotemItem, this.clearTotemItem)
        );

        this.effectColor = new UIColor((color) ->
        {
            if (this.selected == null)
            {
                return;
            }
            String effectId = this.selected.getEffectId().toLowerCase();
            Color c = Color.rgb(color);
            switch (effectId)
            {
                case "fire" -> this.clip.fireColor.set(c);
                case "portal" -> this.clip.portalColor.set(c);
                case "frost" -> this.clip.frostColor.set(c);
                case "underwater" -> this.clip.underwaterColor.set(c);
            }
            this.editor.fillData();
        });

        this.colorSection = UI.column(
            UI.label(IKey.constant("Tint Color")),
            this.effectColor
        );

        this.effectsList = new UIScreenEffectList(this, (list) ->
        {
            List<ScreenEffectEntry> current = this.effectsList.getCurrent();
            this.selected = current == null || current.isEmpty() ? null : current.get(0);
            this.updateDetailSections();
        });
        this.effectsList.h(160);
    }

    public void updateKeyframeSheets()
    {
        this.keyframes.view.removeAllSheets();

        List<String> activeEffects = this.clip.getActiveEffectList();
        int colorIdx = 0;

        for (String effectId : activeEffects)
        {
            ScreenEffectPresetEntry preset = ScreenEffectPresets.getById(effectId);
            String title = preset != null ? preset.name : effectId.toUpperCase();
            int folderColor = UIKeyframeEditor.COLORS[colorIdx++ % UIKeyframeEditor.COLORS.length];

            Icon sectionIcon = switch (effectId.toLowerCase())
            {
                case "vignette" -> Icons.OUTLINE_SPHERE;
                case "night_vision" -> Icons.LIGHT;
                case "blindness" -> Icons.INVISIBLE;
                case "darkness" -> Icons.SPHERE;
                case "spyglass" -> Icons.LOOKING;
                case "frost" -> Icons.SNOWFLAKE;
                case "portal" -> Icons.MAZE;
                case "fire" -> Icons.SUN;
                case "pumpkin" -> Icons.HEART_ALT;
                case "suffocation" -> Icons.BLOCK;
                case "totem" -> Icons.HEART;
                case "underwater" -> Icons.DROP;
                case "nausea" -> Icons.BUBBLE;
                default -> Icons.IMAGE;
            };

            UIKeyframeSheet.Section section = new UIKeyframeSheet.Section("screen_effect/" + effectId, IKey.constant(title), sectionIcon, folderColor);

            // Add dedicated keyframe channels for this effect
            switch (effectId.toLowerCase())
            {
                case "vignette" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.vignetteVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Opacity"), 0xffffff, this.clip.vignetteOpacity, null).icon(Icons.FADING));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_color", IKey.constant("Tint Color"), 0x55ff55, this.clip.vignetteColor, null).icon(Icons.COLOR).seed(Color::new));
                }
                case "night_vision" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.nightVisionVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Brightness / Opacity"), 0xffff55, this.clip.nightVisionOpacity, null).icon(Icons.LIGHT));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_flash", IKey.constant("Flash"), 0xffffff, this.clip.nightVisionFlash, null).icon(Icons.FADING));
                }
                case "blindness" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.blindnessVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Blindness Opacity"), 0x333333, this.clip.blindnessOpacity, null).icon(Icons.FADING));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_radius", IKey.constant("Circle Radius (Blocks)"), 0x555555, this.clip.blindnessRadius, null).icon(Icons.SPHERE));
                }
                case "darkness" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.darknessVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Darkness Opacity / Pulse"), 0x333333, this.clip.darknessOpacity, null).icon(Icons.FADING));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_radius", IKey.constant("Circle Radius (Blocks)"), 0x555555, this.clip.darknessRadius, null).icon(Icons.SPHERE));
                }
                case "spyglass" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.spyglassVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_scale", IKey.constant("Spyglass Scale"), 0x55ffff, this.clip.spyglassScale, null).icon(Icons.SCALE).seed(() -> 1.12F));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_zoom", IKey.constant("Zoom"), 0xffff55, this.clip.spyglassZoom, null).icon(Icons.SEARCH).seed(() -> 0.102F));
                }
                case "frost" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.frostVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_progress", IKey.constant("Frost Progress"), 0x55ffff, this.clip.frostProgress, null).icon(Icons.SNOWFLAKE).seed(() -> 1.0F));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_zoom", IKey.constant("Zoom Multiplier"), 0x55aaff, this.clip.frostZoom, null).icon(Icons.SEARCH).seed(() -> 1.0F));
                }
                case "portal" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.portalVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Portal Opacity"), 0xaa00aa, this.clip.portalOpacity, null).icon(Icons.FADING));
                }
                case "fire" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.fireVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                }
                case "pumpkin" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.pumpkinVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Pumpkin Opacity"), 0xffaa00, this.clip.pumpkinOpacity, null).icon(Icons.FADING));
                }
                case "suffocation" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.suffocationVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_block", IKey.constant("Block ID"), 0xcccccc, this.clip.suffocationBlock, null).icon(Icons.BLOCK).seed(() -> "minecraft:stone"));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Suffocation Opacity"), 0x888888, this.clip.suffocationOpacity, null).icon(Icons.FADING));
                }
                case "totem" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.totemVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_progress", IKey.constant("Pop Progress"), 0xffff55, this.clip.totemProgress, null).icon(Icons.PLAY).seed(() -> 0.0F));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_flipped", IKey.constant("Flipped (Left Hand)"), 0x55aaff, this.clip.totemFlipped, null).icon(Icons.FLIP_HORIZONTAL).seed(() -> false));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_particles", IKey.constant("Totem Particles"), 0x55ffff, this.clip.totemParticles, null).icon(Icons.PARTICLE).seed(() -> true));
                }
                case "underwater" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.underwaterVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Underwater Opacity"), 0x55aaff, this.clip.underwaterOpacity, null).icon(Icons.DROP).seed(() -> 0.1F));
                }
                case "nausea" -> {
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_visible", IKey.constant("Visible"), 0x55ff55, this.clip.nauseaVisible, null).icon(Icons.VISIBLE).seed(() -> true));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_distortion", IKey.constant("Distortion"), 0x55ff55, this.clip.nauseaDistortion, null).icon(Icons.CURVES));
                    this.addSheet(section, new UIKeyframeSheet(effectId + "_opacity", IKey.constant("Overlay Opacity"), 0x55ff55, this.clip.nauseaOpacity, null).icon(Icons.FADING));
                }
            }
        }
    }

    private void addSheet(UIKeyframeSheet.Section section, UIKeyframeSheet sheet)
    {
        if (sheet != null)
        {
            sheet.section = section;
            this.keyframes.view.addSheet(sheet);
        }
    }

    public void openAddEffectMenu(UIContext context)
    {
        context.replaceContextMenu((m) ->
        {
            if (this.selected != null)
            {
                m.icons.add(new MenuIcon(MenuVerb.REMOVE, () ->
                {
                    this.clip.removeEffect(this.selected);
                    this.selected = null;
                    this.refreshList();
                    this.updateKeyframeSheets();
                    this.editor.fillData();
                }));
            }

            m.action(Icons.SUN, IKey.constant("Environment & Vision"), () ->
                context.replaceContextMenu((sub) -> this.populateCategoryMenu(context, sub, List.of(
                    ScreenEffectPresets.VIGNETTE,
                    ScreenEffectPresets.NIGHT_VISION,
                    ScreenEffectPresets.BLINDNESS,
                    ScreenEffectPresets.DARKNESS,
                    ScreenEffectPresets.SPYGLASS))));

            m.action(Icons.CLOSE, IKey.constant("Hazards & Environment"), () ->
                context.replaceContextMenu((sub) -> this.populateCategoryMenu(context, sub, List.of(
                    ScreenEffectPresets.FIRE,
                    ScreenEffectPresets.UNDERWATER,
                    ScreenEffectPresets.FROST,
                    ScreenEffectPresets.PORTAL,
                    ScreenEffectPresets.SUFFOCATION,
                    ScreenEffectPresets.PUMPKIN))));

            m.action(Icons.SAVED, IKey.constant("Status & Popups"), () ->
                context.replaceContextMenu((sub) -> this.populateCategoryMenu(context, sub, List.of(
                    ScreenEffectPresets.NAUSEA,
                    ScreenEffectPresets.TOTEM))));
        });
    }

    private void populateCategoryMenu(UIContext context, mchorse.bbs_mod.ui.utils.context.ContextMenuManager m, List<ScreenEffectPresetEntry> presets)
    {
        for (ScreenEffectPresetEntry preset : presets)
        {
            if (!this.clip.hasEffect(preset.id))
            {
                m.actions.add(new ScreenEffectContextAction(preset, () ->
                {
                    ScreenEffectEntry entry = new ScreenEffectEntry(preset.id);
                    this.clip.addEffect(entry);
                    this.selected = entry;
                    this.refreshList();
                    this.updateKeyframeSheets();
                    this.editor.fillData();
                }));
            }
        }
    }

    private void refreshList()
    {
        this.effectsList.setList(this.clip.getEffects());
        if (this.selected != null && this.clip.getEffects().contains(this.selected))
        {
            this.effectsList.setCurrentScroll(this.selected);
        }
        else if (!this.clip.getEffects().isEmpty())
        {
            this.selected = this.clip.getEffects().get(0);
            this.effectsList.setCurrentScroll(this.selected);
        }
        else
        {
            this.selected = null;
        }
        this.updateKeyframeSheets();
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Screen Effects"),
            this.editKeyframes,
            this.effectsList,
            this.totemSection,
            this.colorSection));
    }

    private void openTotemItemPicker()
    {
        UIContext context = this.getContext();
        if (context == null)
        {
            return;
        }

        String currentId = this.clip.totemItem.get();
        ItemStack currentStack = createItemStack(currentId != null && !currentId.isEmpty() ? currentId : "minecraft:totem_of_undying");

        UIUnifiedPickOverlayPanel panel = UIUnifiedPickOverlayPanel.forItem((stack) ->
        {
            if (stack != null && !stack.isEmpty())
            {
                String id = Registries.ITEM.getId(stack.getItem()).toString();
                this.clip.totemItem.set(id);
                this.fillData();
                this.editor.fillData();
            }
        }, currentStack);

        UIOverlay.addOverlay(context, panel, 280, 240);
    }

    private static ItemStack createItemStack(String id)
    {
        if (id == null || id.isEmpty())
        {
            return new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING);
        }
        try
        {
            net.minecraft.item.Item item = Registries.ITEM.get(new Identifier(id));
            if (item != null && item != net.minecraft.item.Items.AIR)
            {
                return new ItemStack(item);
            }
        }
        catch (Exception ignored)
        {
        }
        return new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING);
    }

    private void updateDetailSections()
    {
        boolean isTotem = this.selected != null && "totem".equalsIgnoreCase(this.selected.getEffectId());
        this.totemSection.setVisible(isTotem);
        if (isTotem)
        {
            String itemId = this.clip.totemItem.get();
            if (itemId != null && !itemId.isEmpty())
            {
                String shortName = itemId.replace("minecraft:", "");
                this.pickTotemItem.label = IKey.constant("Item: " + shortName);
                this.clearTotemItem.setVisible(!"minecraft:totem_of_undying".equals(itemId));
            }
            else
            {
                this.pickTotemItem.label = IKey.constant("Pick Item");
                this.clearTotemItem.setVisible(false);
            }
        }

        boolean isColorable = this.selected != null && isColorableEffect(this.selected.getEffectId());
        this.colorSection.setVisible(isColorable);
        if (isColorable)
        {
            String effectId = this.selected.getEffectId().toLowerCase();
            Color c = switch (effectId)
            {
                case "fire" -> this.clip.fireColor.get();
                case "portal" -> this.clip.portalColor.get();
                case "frost" -> this.clip.frostColor.get();
                case "underwater" -> this.clip.underwaterColor.get();
                default -> Color.white();
            };
            if (c != null)
            {
                this.effectColor.picker.setColor(c.getRGBColor());
            }
        }
    }

    private static boolean isColorableEffect(String id)
    {
        if (id == null) return false;
        String lower = id.toLowerCase();
        return "fire".equals(lower) || "portal".equals(lower) || "frost".equals(lower) || "underwater".equals(lower);
    }

    @Override
    public void fillData()
    {
        super.fillData();
        currentClip = this.clip;
        currentPanel = this;
        this.refreshList();
        this.updateDetailSections();
    }
}
