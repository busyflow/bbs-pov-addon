package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import mchorse.bbs_mod.film.replays.tracks.TrackCatalog;
import mchorse.bbs_mod.film.replays.tracks.TrackDescriptor;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UIGeneralFormPanel.class},
   remap = false
)
public abstract class UIGeneralFormPanelPovMixin extends UIFormPanel {
   @Shadow
   public UIPropTransform transform;
   @Shadow
   public UIToggle hitbox;
   @Shadow
   public UITrackpad hp;

   public UIGeneralFormPanelPovMixin(UIForm editor) {
      super(editor);
   }

   @Inject(
      method = {"startEdit"},
      at = {@At("TAIL")}
   )
   private void bbsPov$hideSectionsInPov(Form form, CallbackInfo info) {
      boolean isPov = UIPovHandEditor.isActive();
      if (!isPov && PovReplaySettings.getFilmPanel() instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null) {
         isPov = access.bbsPov$getEditor().isPoseGizmoSection();
      }

      if (this.options != null) {
         UISection transformSection = this.transform != null ? (UISection)(Object)this.transform.getParent(UISection.class) : null;
         UISection hitboxSection = this.hitbox != null ? (UISection)(Object)this.hitbox.getParent(UISection.class) : null;
         UISection movementSection = this.hp != null ? (UISection)(Object)this.hp.getParent(UISection.class) : null;
         if (transformSection != null) {
            transformSection.setVisible(!isPov);
         }

         if (hitboxSection != null) {
            hitboxSection.setVisible(!isPov);
         }

         if (movementSection != null) {
            movementSection.setVisible(!isPov);
         }

         this.options.resize();
      }
   }

   @Inject(
      method = {"openTrackFilter"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$openTrackFilter(CallbackInfo info) {
      if (this.form != null) {
         boolean isHandEditor = UIPovHandEditor.isActive();
         UIFilmPanel panel = PovReplaySettings.getFilmPanel();
         UIPovEditor povEditor = null;
         if (panel instanceof UIFilmPanelPovAccess access) {
            povEditor = access.bbsPov$getEditor();
         }

         if (isHandEditor || povEditor != null && povEditor.isPoseGizmoSection()) {
            info.cancel();
            this.openFormTrackFilter(this.form, povEditor);
         }
      }
   }

   private void openFormTrackFilter(Form form, UIPovEditor povEditor) {
      Set<String> disabled = (Set<String>)form.disabledTracks.get();
      Set<String> keys = new LinkedHashSet<>();
      Map<String, Integer> keyToColor = new HashMap<>();

      for (TrackDescriptor track : TrackCatalog.of(form)) {
         keys.add(track.filterKey());
         keyToColor.put(track.filterKey(), track.color());
      }

      UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(disabled, keys, keyToColor);
      UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);
      panel.onClose(e -> {
         form.disabledTracks.set(disabled);
         if (povEditor != null) {
            povEditor.refreshSheets(false);
         }
      });
   }
}
