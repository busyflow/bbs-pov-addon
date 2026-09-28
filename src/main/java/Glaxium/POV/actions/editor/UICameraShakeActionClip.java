package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UICameraShakeActionClip extends UIPovActionClip<CameraShakePovActionClip> {
   public UIButton editKeyframes;
   public UIKeyframeEditor keyframes;

   public UICameraShakeActionClip(CameraShakePovActionClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.keyframes = new UIKeyframeEditor(consumer -> new UIFilmKeyframes(this.editor, consumer));
      this.keyframes.view.duration(() -> (Integer)((CameraShakePovActionClip)this.clip).duration.get());
      this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), button -> {
         this.updateKeyframeSheets();
         this.editor.embedView(this.keyframes);
         this.keyframes.view.resetView();
         if (this.keyframes.view.getGraph() != null) {
            this.keyframes.view.getGraph().clearSelection();
         }
      });
   }

   private void updateKeyframeSheets() {
      this.keyframes.view.removeAllSheets();
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("active", IKey.constant("Active"), 16733559, ((CameraShakePovActionClip)this.clip).active, null)
               .icon(Icons.PLAY)
               .seed(() -> true)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("hurt_time", IKey.constant("Hurt Time"), 16746564, ((CameraShakePovActionClip)this.clip).hurtTime, null)
               .icon(Icons.TIME)
               .seed(() -> 10)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("max_hurt_time", IKey.constant("Max Hurt Time"), 13395507, ((CameraShakePovActionClip)this.clip).maxHurtTime, null)
               .icon(Icons.TIME)
               .seed(() -> 10)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("damage_tilt_yaw", IKey.constant("Damage Tilt Yaw"), 4891615, ((CameraShakePovActionClip)this.clip).damageTiltYaw, null)
               .icon(Icons.LOOKING)
               .seed(() -> 0.0F)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("death_time", IKey.constant("Death Time"), 8947848, ((CameraShakePovActionClip)this.clip).deathTime, null)
               .icon(Icons.CLOSE)
               .seed(() -> 0)
         );
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels.add(this.section(IKey.constant("Camera Shake Keyframes"), new UIElement[]{this.editKeyframes}));
   }

   @Override
   public void fillData() {
      super.fillData();
      this.updateKeyframeSheets();
      if (this.keyframes != null && this.keyframes.view != null && this.keyframes.view.getGraph() != null) {
         this.keyframes.view.getGraph().clearSelection();
      }
   }

   public void render(UIContext context) {
      if (this.keyframes != null
         && !this.keyframes.hasParent()
         && this.keyframes.view != null
         && this.keyframes.view.getGraph() != null
         && this.keyframes.view.getGraph().getSelected() != null) {
         this.keyframes.view.getGraph().clearSelection();
      }

      super.render(context);
   }
}
