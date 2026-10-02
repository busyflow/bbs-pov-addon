package wemppy.bbs_pov.client.clips.ui;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIUnifiedPickOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import wemppy.bbs_pov.clips.ScreenEffectsClip;

public class UIScreenEffectsClip extends UIClip<ScreenEffectsClip>
{
    public UIButton editKeyframes;
    public UIKeyframeEditor keyframes;

    public UIButton effectTypeButton;
    public UIButton itemPickButton;
    public UIButton itemResetButton;

    public UIToggle showParticles;
    public UIToggle playSound;
    public UITrackpad scaleMultiplier;

    public UIScreenEffectsClip(ScreenEffectsClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.rulerRenderer((context) ->
        {
            if (this.editor instanceof UIClipsPanel panel && this.clip.getParent() instanceof Clips clips)
            {
                UIReplaysEditor.renderRuler(context, this.keyframes.view, panel, clips, this.clip.tick.get());
            }
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("screen_effects_keyframes");

        UIKeyframeSheet sheetItem = new UIKeyframeSheet("totem_item", IKey.raw("Totem Item"), Colors.YELLOW, this.clip.itemChannel, this.clip.item).icon(Icons.MATERIAL);
        UIKeyframeSheet sheetProg = new UIKeyframeSheet("progress", IKey.raw("Progress"), Colors.GREEN, this.clip.progressChannel, null).icon(Icons.PLAY);
        UIKeyframeSheet sheetScale = new UIKeyframeSheet("scale", IKey.raw("Scale"), Colors.CYAN, this.clip.scaleChannel, null).icon(Icons.MAXIMIZE);

        this.keyframes.view.addSheet(sheetItem);
        this.keyframes.view.addSheet(sheetProg);
        this.keyframes.view.addSheet(sheetScale);

        this.editKeyframes = new UIButton(IKey.raw("Edit Keyframes"), (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.editKeyframes.keys().register(Keys.FORMS_EDIT, () -> this.editKeyframes.clickItself());

        this.effectTypeButton = new UIButton(IKey.raw("Totem of Undying"), (b) ->
        {
            String cur = this.clip.effectType.get();
            if ("totem".equals(cur)) this.clip.effectType.set("potion_swirls");
            else if ("potion_swirls".equals(cur)) this.clip.effectType.set("damage");
            else if ("damage".equals(cur)) this.clip.effectType.set("freeze");
            else if ("freeze".equals(cur)) this.clip.effectType.set("portal");
            else this.clip.effectType.set("totem");
            updateLabels();
        });

        this.itemPickButton = new UIButton(IKey.raw("Item: totem_of_undying"), (b) ->
        {
            UIUnifiedPickOverlayPanel panel = UIUnifiedPickOverlayPanel.forItem((stack) ->
            {
                this.clip.item.set(stack == null || stack.isEmpty() ? new ItemStack(Items.TOTEM_OF_UNDYING) : stack);
                updateLabels();
            }, this.clip.item.get());

            UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.75F);
        });

        this.itemResetButton = new UIButton(IKey.raw("X"), (b) ->
        {
            this.clip.item.set(new ItemStack(Items.TOTEM_OF_UNDYING));
            updateLabels();
        });

        this.showParticles = this.toggle(IKey.raw("Particles"), this.clip.showParticles);
        this.playSound = this.toggle(IKey.raw("Play Sound"), this.clip.playSound);
        this.scaleMultiplier = this.trackpad(this.clip.scaleMultiplier);

        updateLabels();
    }

    private void updateLabels()
    {
        String cur = this.clip.effectType.get();
        if ("totem".equals(cur)) this.effectTypeButton.label = IKey.raw("Totem of Undying");
        else if ("potion_swirls".equals(cur)) this.effectTypeButton.label = IKey.raw("Potion Swirls");
        else if ("damage".equals(cur)) this.effectTypeButton.label = IKey.raw("Damage Vignette");
        else if ("freeze".equals(cur)) this.effectTypeButton.label = IKey.raw("Freeze Overlay");
        else if ("portal".equals(cur)) this.effectTypeButton.label = IKey.raw("Portal Overlay");

        ItemStack stack = this.clip.item.get();
        if (stack != null && !stack.isEmpty())
        {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            this.itemPickButton.label = IKey.raw("Item: " + id.getPath());
        }
        else
        {
            this.itemPickButton.label = IKey.raw("Item: totem_of_undying");
        }
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Screen Effects"), this.editKeyframes, this.effectTypeButton));

        UIElement itemRow = new UIElement();
        itemRow.row(5).add(this.itemPickButton.w(0.8F), this.itemResetButton.w(0.2F));
        this.panels.add(this.section(IKey.raw("Totem Item"), itemRow));

        this.panels.add(this.section(IKey.raw("Settings"), this.showParticles, this.playSound, this.scaleMultiplier));
    }
}
