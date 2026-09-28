package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import java.util.stream.Collectors;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class UIParticleEffectActionClip extends UIPovActionClip<ParticleEffectPovActionClip> {
   public UIButton pickParticle;

   public UIParticleEffectActionClip(ParticleEffectPovActionClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.pickParticle = new UIButton(IKey.constant("Particle"), button -> this.openParticlePicker());
      this.pickParticle.tooltip(IKey.constant((String)((ParticleEffectPovActionClip)this.clip).particle.get()));
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels.add(this.section(IKey.constant("Particle Effect"), new UIElement[]{this.pickParticle}));
   }

   @Override
   public void fillData() {
      super.fillData();
      this.pickParticle.tooltip(IKey.constant((String)((ParticleEffectPovActionClip)this.clip).particle.get()));
   }

   private void openParticlePicker() {
      UIContext context = this.getContext();
      if (context != null) {
         UIListOverlayPanel panel = new UIListOverlayPanel(IKey.constant("Choose Particle"), value -> {
            this.editor.editMultiple(((ParticleEffectPovActionClip)this.clip).particle, v -> v.set(value));
            this.pickParticle.tooltip(IKey.constant(value));
         });
         panel.addValues(Registries.PARTICLE_TYPE.getIds().stream().map(Identifier::toString).sorted().collect(Collectors.toList()));
         UIOverlay.addOverlay(context, panel, 0.45F, 0.7F);
      }
   }
}
