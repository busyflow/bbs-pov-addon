package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.particle.ParticleEffects;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UIParticleEffectActionClip extends UIPovActionClip<ParticleEffectPovActionClip>
{
    public UIButton pickParticle;
    public UIToggle rainbow;
    public UIColor customColor;
    public UIElement colorRow;

    public UIParticleEffectActionClip(ParticleEffectPovActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.pickParticle = new UIButton(
            IKey.constant(ParticleEffects.displayName(this.clip.particle.get())),
            (button) -> this.openParticlePicker());

        this.rainbow = this.toggle(IKey.constant("Rainbow"), this.clip.rainbow);
        this.rainbow.tooltip(IKey.constant("Smoothly cycle particle color through a rainbow"));

        this.customColor = new UIColor((c) ->
        {
            int rgb = c & 0xFFFFFF;
            this.editor.editMultiple(this.clip.color, (v) -> v.set(rgb));
        });
        this.customColor.tooltip(IKey.constant("Custom particle color"));
        this.bind(this.customColor, () -> this.customColor.setColor(this.clip.getColorRgb()));

        this.colorRow = UI.labelRow(IKey.constant("Color"), 50, this.customColor);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();
        this.panels.add(this.section(IKey.constant("Particle Effect"), this.pickParticle, this.rainbow, this.colorRow));
    }

    @Override
    public void fillData()
    {
        super.fillData();
        this.updateColorControls();
    }

    private void updateColorControls()
    {
        String particleId = this.clip.particle.get();
        boolean colorable = ParticleEffects.supportsCustomColor(this.clip);

        this.pickParticle.label = IKey.constant(ParticleEffects.displayName(particleId));
        this.pickParticle.tooltip(IKey.constant(particleId));

        this.rainbow.setVisible(colorable);
        this.colorRow.setVisible(colorable);

        if (!colorable && this.customColor.picker.hasParent())
        {
            this.customColor.picker.removeFromParent();
        }

        if (this.panels != null)
        {
            this.panels.resize();
        }
    }

    private void openParticlePicker()
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        UIListOverlayPanel panel = new UIListOverlayPanel(IKey.constant("Choose Particle"), (value) ->
        {
            String id = ParticleEffects.fromDisplayName(value);
            this.editor.editMultiple(this.clip.particle, (v) -> v.set(id));
            this.updateColorControls();
        });

        List<String> values = new ArrayList<>();
        values.add(ParticleEffectPovActionClip.CUSTOM_COLORED_EFFECT_NAME);
        values.addAll(Registries.PARTICLE_TYPE.getIds().stream()
            .map(Identifier::toString)
            .sorted()
            .collect(Collectors.toList()));

        panel.addValues(values);
        panel.setValue(ParticleEffects.displayName(this.clip.particle.get()));
        UIOverlay.addOverlay(context, panel, 0.45F, 0.7F);
    }
}
