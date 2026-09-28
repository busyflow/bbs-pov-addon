package Glaxium.POV.hand.editor;

import Glaxium.POV.render.PovViewportMetrics;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIModelKeyframeFactory extends UIKeyframeFactory<String> {
   private final UIButton pickModel;
   private String currentModel;

   public UIModelKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor) {
      super(keyframe, editor);
      this.currentModel = (String)keyframe.getValue();
      if (this.currentModel == null) {
         this.currentModel = "";
      }

      this.pickModel = new UIButton(IKey.constant(this.getButtonLabel()), this::onPickModelClicked);
      this.scroll.add(this.pickModel);
   }

   private String getButtonLabel() {
      return this.currentModel != null && !this.currentModel.isBlank() ? "Model: " + this.currentModel : "Pick Model";
   }

   private void updateButtonLabel() {
      this.pickModel.label = IKey.constant(this.getButtonLabel());
   }

   private void onPickModelClicked(UIButton button) {
      ModelForm currentForm = new ModelForm();
      if (this.currentModel != null && !this.currentModel.isBlank()) {
         currentForm.model.set(this.currentModel);
      }

      UIElement parent = null;
      UIFilmPanel filmPanel = PovViewportMetrics.resolveFilmPanel();
      if (filmPanel != null) {
         parent = filmPanel;
      } else if (this.getRoot() != null) {
         parent = this.getParentContainer();
      }

      if (parent == null) {
         parent = this.getParent();
      }

      if (parent == null) {
         parent = this;
      }

      UIFormPalette palette = UIFormPalette.open(parent, false, currentForm, form -> {
         if (form != null) {
            if (FormUtils.getRoot(form) instanceof ModelForm mf) {
               String pickedModel = (String)mf.model.get();
               if (pickedModel != null && !pickedModel.isBlank()) {
                  this.setModel(pickedModel);
               }
            }
         }
      });
      if (palette != null) {
         palette.updatable();
      }
   }

   private void setModel(String model) {
      this.currentModel = model == null ? "" : model;
      this.setValue(this.currentModel);
      this.updateButtonLabel();
   }

   public void update() {
      super.update();
      String val = (String)this.keyframe.getValue();
      if (val != null && !val.equals(this.currentModel)) {
         this.currentModel = val;
         this.updateButtonLabel();
      }
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      super.render(context);
   }
}
