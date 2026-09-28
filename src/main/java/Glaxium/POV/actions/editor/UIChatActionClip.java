package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.ChatPovActionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIChatActionClip extends UIPovActionClip<ChatPovActionClip> {
   public UIToggle showRecommendations;
   public UIButton editKeyframes;
   public UIKeyframeEditor keyframes;

   public UIChatActionClip(ChatPovActionClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.keyframes = new UIKeyframeEditor(consumer -> new UIFilmKeyframes(this.editor, consumer));
      this.keyframes.view.duration(() -> (Integer)((ChatPovActionClip)this.clip).duration.get());
      this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), button -> {
         this.updateKeyframeSheets();
         this.editor.embedView(this.keyframes);
         this.keyframes.view.resetView();
         if (this.keyframes.view.getGraph() != null) {
            this.keyframes.view.getGraph().clearSelection();
         }
      });
      this.showRecommendations = new UIToggle(IKey.constant("Command Recommendations"), toggle -> {
         ((ChatPovActionClip)this.clip).showRecommendations.set(toggle.getValue());
         this.editor.fillData();
      });
   }

   private void updateKeyframeSheets() {
      this.keyframes.view.removeAllSheets();
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("chat_text", IKey.constant("Chat Text"), 16777215, ((ChatPovActionClip)this.clip).text, null).icon(Icons.FONT).seed(() -> "")
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("chat_bar_visible", IKey.constant("Background Bar / Cursor"), 5635925, ((ChatPovActionClip)this.clip).barVisible, null)
               .icon(Icons.LAYOUT)
               .seed(() -> true)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("chat_cursor_pos", IKey.constant("Cursor Position"), 5636095, ((ChatPovActionClip)this.clip).cursorPos, null)
               .icon(Icons.POINTER)
               .seed(() -> 0)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("executed_text", IKey.constant("Executed Text"), 16755200, ((ChatPovActionClip)this.clip).executedText, null)
               .icon(Icons.FONT)
               .seed(() -> "")
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("chat_scroll", IKey.constant("Chat Scroll"), 5614335, ((ChatPovActionClip)this.clip).chatScroll, null)
               .icon(Icons.MORE)
               .seed(() -> 0)
         );
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels.add(this.section(IKey.constant("Chat"), new UIElement[]{this.editKeyframes, this.showRecommendations}));
   }

   @Override
   public void fillData() {
      super.fillData();
      ((ChatPovActionClip)this.clip).createDefaultKeyframes();
      this.showRecommendations.setValue((Boolean)((ChatPovActionClip)this.clip).showRecommendations.get());
      this.updateKeyframeSheets();
   }
}
