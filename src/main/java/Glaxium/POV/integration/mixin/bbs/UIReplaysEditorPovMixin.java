package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {UIReplaysEditor.class},
   remap = false
)
public class UIReplaysEditorPovMixin {
   @Shadow
   private UIFilmPanel filmPanel;

   @Inject(
      method = {"selectBodyPart"},
      at = {@At("HEAD")}
   )
   private void bbsPov$onSelectBodyPart(String path, CallbackInfo info) {
      if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$isPovActive()) {
         UIPovEditor povEditor = access.bbsPov$getEditor();
         if (povEditor != null) {
            povEditor.selectBodyPart(path);
         }
      }
   }

   @Inject(
      method = {"pickFormBone"},
      at = {@At("HEAD")}
   )
   private void bbsPov$selectOwnerReplayOnPick(Form form, String bone, boolean insert, CallbackInfo info) {
      if (form != null && this.filmPanel != null && this.filmPanel.getData() != null) {
         UIReplaysEditor self = (UIReplaysEditor)(Object)this;
         Form root = FormUtils.getRoot(form);
         Film film = (Film)(Object)this.filmPanel.getData();

         for (Replay r : film.replays.getList()) {
            if (r != null && r.form.get() != null) {
               Form rRoot = FormUtils.getRoot((Form)r.form.get());
               if (r.form.get() == root || rRoot == root) {
                  if (self.getReplay() != r) {
                     self.setReplay(r);
                  }
                  break;
               }
            }
         }
      }
   }

   @Inject(
      method = {"updateChannelsList"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$preventReplayEditorUpdateInPov(CallbackInfo info) {
      if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$isPovActive()) {
         info.cancel();
      }
   }

   @Inject(
      method = {"collectCuratedSheets"},
      at = {@At("RETURN")}
   )
   private void bbsPov$hideHotbarSlotsInReplayEditor(List<UIKeyframeSheet> sheets, CallbackInfo info) {
      sheets.removeIf(sheet -> bbsPov$isPovOwnedReplaySheet(sheet.id));
   }

   private static boolean bbsPov$isPovOwnedReplaySheet(String id) {
      if (!"item_off_hand".equals(id) && !"selected_slot".equals(id)) {
         for (int i = 0; i < 9; i++) {
            if (ReplayKeyframes.hotbarChannelId(i).equals(id)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
