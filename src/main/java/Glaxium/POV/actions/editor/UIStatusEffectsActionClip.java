package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.statuseffect.StatusEffectEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.context.MenuIcon;
import mchorse.bbs_mod.ui.utils.context.MenuVerb;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Inspector panel for StatusEffectsPovActionClip.
 */
public class UIStatusEffectsActionClip extends UIPovActionClip<StatusEffectsPovActionClip>
{
    public static class StatusEffectContextAction extends ContextAction
    {
        private final StatusEffect effect;

        public StatusEffectContextAction(StatusEffect effect, IKey label, Runnable runnable)
        {
            super(Icons.NONE, label, runnable);
            this.effect = effect;
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

            // Black box for icon
            context.batcher.box(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xCC000000);

            if (this.effect != null && context.batcher.getContext() != null)
            {
                Sprite sprite = MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(this.effect);
                if (sprite != null)
                {
                    RenderSystem.enableBlend();
                    context.batcher.getContext().drawSprite(iconX, iconY, 0, 16, 16, sprite);
                }
            }

            context.batcher.text(this.label.get(), x + 24, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);
        }
    }

    public static class UIStatusEffectList extends UIList<StatusEffectEntry>
    {
        private final UIStatusEffectsActionClip parent;

        public UIStatusEffectList(UIStatusEffectsActionClip parent, Consumer<List<StatusEffectEntry>> callback)
        {
            super(callback);
            this.parent = parent;
            this.scroll.scrollItemSize = 22;
            this.sorting();
            this.emptyState(IKey.constant("Right-click to add effects"));
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
        protected void handleSwap(int from, int to)
        {
            super.handleSwap(from, to);
            this.parent.syncOrderFromList();
        }

        @Override
        public void render(UIContext context)
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xCC141414);
            context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0x33FFFFFF);
            super.render(context);
        }

        @Override
        public void renderListElement(UIContext context, StatusEffectEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            RowStyle.row(context.batcher, x, y, this.area.w, h, this.rowColor(element), this.isHeader(element), hover, selected);

            if (this.drag.isTarget(element))
            {
                RowStyle.dropTarget(context.batcher, x, y, this.area.w, h);
            }

            this.renderElementPart(context, element, i, x, y, hover, selected);
        }

        @Override
        protected void renderElementPart(UIContext context, StatusEffectEntry element, int i, int x, int y, boolean hover, boolean selected)
        {
            int h = this.scroll.scrollItemSize;
            int iconX = x + 3;
            int iconY = y + (h - 16) / 2;

            // Draw dark background box for the icon
            context.batcher.box(iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xCC000000);

            if (element != null && element.getStatusEffect() != null && context.batcher.getContext() != null)
            {
                Sprite sprite = MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(element.getStatusEffect());
                if (sprite != null)
                {
                    RenderSystem.enableBlend();
                    context.batcher.getContext().drawSprite(iconX, iconY, 0, 16, 16, sprite);
                }
            }

            int textX = x + 24;
            String text = this.elementToString(context, i, element);
            context.batcher.textShadow(text, textX, y + (h - context.batcher.getFont().getHeight()) / 2, RowStyle.textColor(hover || selected));
        }

        @Override
        protected String elementToString(UIContext context, int i, StatusEffectEntry element)
        {
            String level = element.getAmplifier() > 0 ? " " + (element.getAmplifier() + 1) : "";
            return element.getDisplayName() + level;
        }
    }

    public UIStatusEffectList effectsList;

    public UIToggle unlimited;
    public UITrackpad duration;
    public UITrackpad amplifier;

    private StatusEffectEntry selected;

    public UIStatusEffectsActionClip(StatusEffectsPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    public void syncOrderFromList()
    {
        List<StatusEffectEntry> reordered = new ArrayList<>(this.effectsList.getList());
        this.clip.getEffects().clear();
        this.clip.getEffects().addAll(reordered);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.effectsList = new UIStatusEffectList(this, (list) ->
        {
            List<StatusEffectEntry> current = this.effectsList.getCurrent();
            this.selected = current == null || current.isEmpty() ? null : current.get(0);
            this.updateProperties();
        });
        this.effectsList.h(160);

        this.unlimited = new UIToggle(IKey.constant("Unlimited (∞)"), (toggle) ->
        {
            if (this.selected != null)
            {
                this.selected.setUnlimited(toggle.getValue());
                this.updateProperties();
                this.effectsList.update();
                this.editor.fillData();
            }
        });
        this.unlimited.tooltip(IKey.constant("Make this status effect duration infinite (∞)"));

        this.duration = new UITrackpad((value) ->
        {
            if (this.selected != null)
            {
                int sec = Math.max(1, value.intValue());
                this.selected.setDurationSeconds(sec);
                this.updateDurationTooltip();
                this.effectsList.update();
                this.editor.fillData();
            }
        });
        this.duration.limit(1, 72000, true);
        this.duration.setValue(100);

        this.amplifier = new UITrackpad((value) ->
        {
            if (this.selected != null)
            {
                int amp = Math.max(0, value.intValue());
                this.selected.setAmplifier(amp);
                this.effectsList.update();
                this.editor.fillData();
            }
        });
        this.amplifier.limit(0, 255, true);
        this.amplifier.setValue(0);
        this.amplifier.tooltip(IKey.constant("Effect amplifier / level (0 = Level I, 1 = Level II, etc.)"));
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
                    this.editor.fillData();
                }));
            }

            m.action(Icons.HEART, IKey.constant("Beneficial Effects"), () ->
                context.replaceContextMenu((sub) -> this.populateCategoryMenu(context, sub, StatusEffectCategory.BENEFICIAL)));

            m.action(Icons.CLOSE, IKey.constant("Harmful Effects"), () ->
                context.replaceContextMenu((sub) -> this.populateCategoryMenu(context, sub, StatusEffectCategory.HARMFUL)));

            m.action(Icons.BUBBLE, IKey.constant("Harmless Effects"), () ->
                context.replaceContextMenu((sub) -> this.populateHarmlessMenu(context, sub)));
        });
    }

    private void populateCategoryMenu(UIContext context, ContextMenuManager m, StatusEffectCategory category)
    {
        List<StatusEffect> available = new ArrayList<>();
        for (Identifier id : Registries.STATUS_EFFECT.getIds())
        {
            if (!this.clip.hasEffect(id.toString()))
            {
                StatusEffect effect = Registries.STATUS_EFFECT.get(id);
                if (effect != null && effect.getCategory() == category)
                {
                    available.add(effect);
                }
            }
        }

        available.sort((a, b) -> a.getName().getString().compareToIgnoreCase(b.getName().getString()));

        for (StatusEffect effect : available)
        {
            Identifier id = Registries.STATUS_EFFECT.getId(effect);
            if (id == null) continue;

            String name = effect.getName().getString();
            String idStr = id.toString();

            m.actions.add(new StatusEffectContextAction(effect, IKey.constant(name), () ->
            {
                StatusEffectEntry entry = new StatusEffectEntry(idStr, false, 100, 0);
                this.clip.addEffect(entry);
                this.selected = entry;
                this.refreshList();
                this.editor.fillData();
            }));
        }
    }

    private void populateHarmlessMenu(UIContext context, ContextMenuManager m)
    {
        List<StatusEffect> available = new ArrayList<>();
        for (Identifier id : Registries.STATUS_EFFECT.getIds())
        {
            if (!this.clip.hasEffect(id.toString()))
            {
                StatusEffect effect = Registries.STATUS_EFFECT.get(id);
                if (effect != null && effect.getCategory() != StatusEffectCategory.BENEFICIAL && effect.getCategory() != StatusEffectCategory.HARMFUL)
                {
                    available.add(effect);
                }
            }
        }

        available.sort((a, b) -> a.getName().getString().compareToIgnoreCase(b.getName().getString()));

        for (StatusEffect effect : available)
        {
            Identifier id = Registries.STATUS_EFFECT.getId(effect);
            if (id == null) continue;

            String name = effect.getName().getString();
            String idStr = id.toString();

            m.actions.add(new StatusEffectContextAction(effect, IKey.constant(name), () ->
            {
                StatusEffectEntry entry = new StatusEffectEntry(idStr, false, 100, 0);
                this.clip.addEffect(entry);
                this.selected = entry;
                this.refreshList();
                this.editor.fillData();
            }));
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
        this.updateProperties();
    }

    private void updateProperties()
    {
        boolean hasSelected = this.selected != null;
        this.unlimited.setEnabled(hasSelected);
        this.duration.setEnabled(hasSelected && !this.selected.isUnlimited());
        this.amplifier.setEnabled(hasSelected);

        if (hasSelected)
        {
            this.unlimited.setValue(this.selected.isUnlimited());
            this.duration.setValue(this.selected.getDurationSeconds());
            this.amplifier.setValue(this.selected.getAmplifier());
            this.updateDurationTooltip();
        }
    }

    private void updateDurationTooltip()
    {
        if (this.selected != null)
        {
            int sec = this.selected.getDurationSeconds();
            this.duration.tooltip(IKey.constant("Duration: " + this.selected.formatDurationSecondsOnly(sec)));
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(
            IKey.constant("Status Effects"),
            this.effectsList));

        this.panels.add(this.section(
            IKey.constant("Selected Effect"),
            this.unlimited,
            this.duration,
            this.amplifier));
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.refreshList();
    }
}
