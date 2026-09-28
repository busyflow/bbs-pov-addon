package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.item.ItemStack;

public class UIHotbarItemKeyframeFactory extends UIKeyframeFactory<ItemStack> {
   private final UIItemStack itemPicker;
   private final UITrackpad count;
   private ItemStack current;

   public UIHotbarItemKeyframeFactory(Keyframe<ItemStack> keyframe, UIKeyframes editor) {
      super(keyframe, editor);
      ItemStack initial = (ItemStack)keyframe.getValue();
      this.current = initial == null ? ItemStack.EMPTY : initial.copy();
      this.itemPicker = new UIItemStack(this::onItemPicked);
      this.itemPicker.setStack(this.current);
      this.count = new UITrackpad(this::onCountChanged);
      this.count.limit(1.0, 64.0, true);
      this.count.setValue((double)Math.max(1, this.current.getCount()));
      UIKeyframeSheet sheet = null;
      if (editor != null && editor.getGraph() != null) {
         sheet = editor.getGraph().getSheet(keyframe);
      }

      if (sheet == null && editor != null && editor.getDopeSheet() != null) {
         sheet = editor.getDopeSheet().getSheet(keyframe);
      }

      String id = sheet != null && sheet.id != null ? sheet.id : (keyframe.getParent() != null ? keyframe.getParent().getId() : "");
      boolean isBlockChannel = "suffocation_block".equals(id) || id != null && id.endsWith("_block");
      this.scroll.add(this.itemPicker);
      if (!isBlockChannel) {
         this.scroll.add(this.count.marginTop(4));
      }
   }

   private void onItemPicked(ItemStack picked) {
      this.current = picked != null && !picked.isEmpty() ? picked.copy() : ItemStack.EMPTY;
      UIKeyframeSheet sheet = null;
      if (this.editor != null && this.editor.getGraph() != null) {
         sheet = this.editor.getGraph().getSheet(this.keyframe);
      }

      if (sheet == null && this.editor != null && this.editor.getDopeSheet() != null) {
         sheet = this.editor.getDopeSheet().getSheet(this.keyframe);
      }

      String id = sheet != null && sheet.id != null ? sheet.id : (this.keyframe.getParent() != null ? this.keyframe.getParent().getId() : "");
      boolean isBlockChannel = "suffocation_block".equals(id) || id != null && id.endsWith("_block");
      if (!this.current.isEmpty() && !isBlockChannel) {
         this.current.setCount(clampCount(this.count.getValue()));
      }

      this.itemPicker.setStack(this.current);
      this.setValue(this.current);
   }

   private void onCountChanged(double value) {
      if (!this.current.isEmpty()) {
         this.current.setCount(clampCount(value));
         this.itemPicker.setStack(this.current);
         this.setValue(this.current);
      }
   }

   private static int clampCount(double value) {
      return Math.max(1, Math.min(64, (int)value));
   }

   public void render(UIContext context) {
      context.batcher.box((float)this.area.x, (float)this.area.y, (float)this.area.ex(), (float)this.area.ey(), -15461356);
      super.render(context);
   }
}
