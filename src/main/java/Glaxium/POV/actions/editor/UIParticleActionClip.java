package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ParticlePovActionClip;
import java.util.stream.Collectors;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class UIParticleActionClip extends UIPovActionClip<ParticlePovActionClip> {
   public UIButton pickParticle;
   public UITrackpad count;
   public UITrackpad posX;
   public UITrackpad posY;
   public UITrackpad posZ;
   public UITrackpad velX;
   public UITrackpad velY;
   public UITrackpad velZ;
   public UITrackpad spread;
   public UIButton space;
   public UITrackpad seed;
   public UITextbox extraArgs;

   public UIParticleActionClip(ParticlePovActionClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.pickParticle = new UIButton(IKey.constant("Particle"), button -> this.openParticlePicker());
      this.count = new UITrackpad(value -> this.editor.editMultiple(((ParticlePovActionClip)this.clip).count, channel -> {
            if (channel.isEmpty()) {
               channel.insert(0.0F, value.intValue());
            } else {
               channel.get(0).setValue(value.intValue());
            }
         }));
      this.count.limit(0.0, 4096.0, true).forcedLabel(IKey.constant("Count"));
      this.posX = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).positionX, val.floatValue()));
      this.posX.forcedLabel(IKey.constant("Pos X"));
      this.posY = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).positionY, val.floatValue()));
      this.posY.forcedLabel(IKey.constant("Pos Y"));
      this.posZ = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).positionZ, val.floatValue()));
      this.posZ.forcedLabel(IKey.constant("Pos Z"));
      this.velX = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).velocityX, val.floatValue()));
      this.velX.forcedLabel(IKey.constant("Vel X"));
      this.velY = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).velocityY, val.floatValue()));
      this.velY.forcedLabel(IKey.constant("Vel Y"));
      this.velZ = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).velocityZ, val.floatValue()));
      this.velZ.forcedLabel(IKey.constant("Vel Z"));
      this.spread = new UITrackpad(val -> this.editChannel(((ParticlePovActionClip)this.clip).spread, val.floatValue()));
      this.spread.limit(0.0, 64.0, true).forcedLabel(IKey.constant("Spread"));
      this.space = new UIButton(IKey.constant("Space"), button -> {
         int next = ((Integer)((ParticlePovActionClip)this.clip).space.get() + 1) % 3;
         this.editor.editMultiple(((ParticlePovActionClip)this.clip).space, val -> val.set(next));
         this.updateSpaceLabel();
      });
      this.seed = new UITrackpad(val -> this.editor.editMultiple(((ParticlePovActionClip)this.clip).seed, v -> v.set(val.intValue())));
      this.seed.limit(0.0, 2.147483647E9, true).forcedLabel(IKey.constant("Seed"));
      this.extraArgs = new UITextbox(1000, text -> this.editor.editMultiple(((ParticlePovActionClip)this.clip).extraArgs, v -> v.set(text)));
      this.extraArgs.tooltip(IKey.constant("Optional extra parameters for dust, item or block particles"));
   }

   private void editChannel(KeyframeChannel<Float> channel, float value) {
      this.editor.editMultiple(channel, ch -> {
         if (ch.isEmpty()) {
            ch.insert(0.0F, value);
         } else {
            ch.get(0).setValue(value);
         }
      });
   }

   private void updateSpaceLabel() {
      String name = switch (((ParticlePovActionClip)this.clip).space.get()) {
         case 1 -> "Space: Local";
         case 2 -> "Space: World";
         default -> "Space: View";
      };
      this.space.label = IKey.constant(name);
   }

   private void openParticlePicker() {
      UIContext context = this.getContext();
      if (context != null) {
         UIListOverlayPanel panel = new UIListOverlayPanel(IKey.constant("Choose Particle"), value -> {
            this.editor.editMultiple(((ParticlePovActionClip)this.clip).particle, channel -> {
               int index = channel.insert(0.0F, value);
               if (index >= 0) {
                  ((Keyframe)channel.getKeyframes().get(index)).getInterpolation().setInterp(Interpolations.CONST);
               }
            });
            this.pickParticle.label = IKey.constant("Particle: " + value);
         });
         panel.addValues(Registries.PARTICLE_TYPE.getIds().stream().map(Identifier::toString).sorted().collect(Collectors.toList()));
         UIOverlay.addOverlay(context, panel, 0.45F, 0.7F);
      }
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels
         .add(
            this.section(
               IKey.constant("Particle"),
               new UIElement[]{
                  this.pickParticle,
                  UI.row(new UIElement[]{this.count, this.spread}),
                  UI.row(new UIElement[]{this.posX, this.posY, this.posZ}),
                  UI.row(new UIElement[]{this.velX, this.velY, this.velZ}),
                  UI.row(new UIElement[]{this.space, this.seed}),
                  this.extraArgs
               }
            )
         );
   }

   @Override
   public void fillData() {
      super.fillData();
      String particleId = ((ParticlePovActionClip)this.clip).particle.isEmpty()
         ? "minecraft:poof"
         : (String)((ParticlePovActionClip)this.clip).particle.get(0).getValue();
      this.pickParticle.label = IKey.constant("Particle: " + particleId);
      this.count
         .setValue(
            ((ParticlePovActionClip)this.clip).count.isEmpty() ? 1.0 : (double)((Integer)((ParticlePovActionClip)this.clip).count.get(0).getValue()).intValue()
         );
      this.posX
         .setValue(
            ((ParticlePovActionClip)this.clip).positionX.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).positionX.get(0).getValue()).floatValue()
         );
      this.posY
         .setValue(
            ((ParticlePovActionClip)this.clip).positionY.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).positionY.get(0).getValue()).floatValue()
         );
      this.posZ
         .setValue(
            ((ParticlePovActionClip)this.clip).positionZ.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).positionZ.get(0).getValue()).floatValue()
         );
      this.velX
         .setValue(
            ((ParticlePovActionClip)this.clip).velocityX.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).velocityX.get(0).getValue()).floatValue()
         );
      this.velY
         .setValue(
            ((ParticlePovActionClip)this.clip).velocityY.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).velocityY.get(0).getValue()).floatValue()
         );
      this.velZ
         .setValue(
            ((ParticlePovActionClip)this.clip).velocityZ.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).velocityZ.get(0).getValue()).floatValue()
         );
      this.spread
         .setValue(
            ((ParticlePovActionClip)this.clip).spread.isEmpty()
               ? 0.0
               : (double)((Float)((ParticlePovActionClip)this.clip).spread.get(0).getValue()).floatValue()
         );
      this.seed.setValue((double)((Integer)((ParticlePovActionClip)this.clip).seed.get()).intValue());
      this.extraArgs.setText((String)((ParticlePovActionClip)this.clip).extraArgs.get());
      this.updateSpaceLabel();
   }
}
