package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.bossbar.BossBarTypeEntry;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class UIBossBarActionClip extends UIPovActionClip<BossBarPovActionClip> {
   public UIButton bossType;
   public UIButton color;
   public UIButton style;
   public UITextbox name;
   public UIButton editKeyframes;
   public UIKeyframeEditor keyframes;

   public UIBossBarActionClip(BossBarPovActionClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.keyframes = new UIKeyframeEditor(consumer -> new UIFilmKeyframes(this.editor, consumer));
      this.keyframes.view.duration(() -> (Integer)((BossBarPovActionClip)this.clip).duration.get());
      this.editKeyframes = new UIButton(IKey.constant("Edit Keyframes"), button -> {
         this.updateKeyframeSheets();
         this.editor.embedView(this.keyframes);
         this.keyframes.view.resetView();
         if (this.keyframes.view.getGraph() != null) {
            this.keyframes.view.getGraph().clearSelection();
         }
      });
      this.bossType = new UIButton(IKey.constant("Boss"), button -> {
         BossBarTypeEntry next = BossBarTypeEntry.next(((BossBarPovActionClip)this.clip).resolveType());
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).state, channel -> setFirst(channel, next.id));
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).name, channel -> setFirst(channel, next.defaultTitle));
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).color, channel -> setFirst(channel, next.defaultColor));
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).style, channel -> setFirst(channel, next.defaultStyle));
         this.updateButtons();
      });
      this.color = new UIButton(IKey.constant("Color"), button -> {
         String next = BossBarLooks.nextColor(this.current(((BossBarPovActionClip)this.clip).color, "pink"));
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).color, channel -> setFirst(channel, next));
         this.updateButtons();
      });
      this.style = new UIButton(IKey.constant("Style"), button -> {
         String next = BossBarLooks.nextStyle(this.current(((BossBarPovActionClip)this.clip).style, "progress"));
         this.editor.editMultiple(((BossBarPovActionClip)this.clip).style, channel -> setFirst(channel, next));
         this.updateButtons();
      });
      this.name = new UITextbox(10000, text -> this.editor.editMultiple(((BossBarPovActionClip)this.clip).name, channel -> setFirst(channel, text)));
   }

   private void updateKeyframeSheets() {
      this.keyframes.view.removeAllSheets();
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("name", IKey.constant("Name"), 16777215, ((BossBarPovActionClip)this.clip).name, null)
               .icon(Icons.FONT)
               .seed(() -> "Ender Dragon")
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("percent", IKey.constant("Health"), 16733525, ((BossBarPovActionClip)this.clip).percent, null)
               .icon(Icons.HEART)
               .seed(() -> 1.0F)
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("bossbar_color", IKey.constant("Color"), 13386990, ((BossBarPovActionClip)this.clip).color, null)
               .icon(Icons.COLOR)
               .seed(() -> "pink")
         );
      this.keyframes
         .view
         .addSheet(
            new UIKeyframeSheet("bossbar_style", IKey.constant("Style"), 11184810, ((BossBarPovActionClip)this.clip).style, null)
               .icon(Icons.LAYOUT)
               .seed(() -> "progress")
         );
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels.add(this.section(IKey.constant("Boss Bar"), new UIElement[]{this.bossType, this.name, this.color, this.style, this.editKeyframes}));
   }

   @Override
   public void fillData() {
      super.fillData();
      ((BossBarPovActionClip)this.clip).ensureDefaults();
      this.updateButtons();
      this.name.setText(this.current(((BossBarPovActionClip)this.clip).name, "Ender Dragon"));
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

   private void updateButtons() {
      BossBarTypeEntry type = BossBarTypeEntry.findById(((BossBarPovActionClip)this.clip).resolveType());
      String typeName = type == null ? "Ender Dragon" : type.name;
      this.bossType.label = IKey.constant("Boss: " + typeName);
      this.color.label = IKey.constant("Color: " + BossBarLooks.displayColor(this.current(((BossBarPovActionClip)this.clip).color, "pink")));
      this.style.label = IKey.constant("Style: " + BossBarLooks.displayStyle(this.current(((BossBarPovActionClip)this.clip).style, "progress")));
   }

   private String current(KeyframeChannel<String> channel, String fallback) {
      return channel != null && !channel.isEmpty() ? (String)channel.get(0).getValue() : fallback;
   }

   private static <T> void setFirst(KeyframeChannel<T> channel, T value) {
      if (channel.isEmpty()) {
         channel.insert(0.0F, value);
      } else {
         channel.get(0).setValue(value);
      }
   }
}
