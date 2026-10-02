package Glaxium.POV.hud.editor;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.item.ItemStack;

public class UIHotbarItemKeyframeFactory extends UIKeyframeFactory<ItemStack> {
   private final UIItemStack itemPicker;
   private final UITrackpad count;
   private ItemStack current;

   public UIHotbarItemKeyframeFactory(UITrackValue<ItemStack> track, UIKeyframes editor) {
      super(track, editor);
      ItemStack initial = track.getValue();
      this.current = initial == null ? ItemStack.EMPTY : initial.copy();
      this.itemPicker = new UIItemStack(this::onItemPicked);
      this.itemPicker.setStack(this.current);
      this.count = new UITrackpad(this::onCountChanged);
      this.count.limit(1.0, 64.0, true);
      this.count.setValue((double)Math.max(1, this.current.getCount()));
      UIKeyframeSheet sheet = track.sheet;
      String id = sheet != null && sheet.id != null ? sheet.id : (sheet != null && sheet.channel != null ? sheet.channel.getId() : "");
      boolean isBlockChannel = "suffocation_block".equals(id) || id != null && id.endsWith("_block");
      this.scroll.add(this.itemPicker);
      if (!isBlockChannel) {
         this.scroll.add(this.count.marginTop(4));
      }
   }

   private void onItemPicked(ItemStack picked) {
      this.current = picked != null && !picked.isEmpty() ? picked.copy() : ItemStack.EMPTY;
      UIKeyframeSheet sheet = this.track.sheet;
      String id = sheet != null && sheet.id != null ? sheet.id : (sheet != null && sheet.channel != null ? sheet.channel.getId() : "");
      boolean isBlockChannel = "suffocation_block".equals(id) || id != null && id.endsWith("_block");
      if (!this.current.isEmpty() && !isBlockChannel) {
         this.current.setCount(clampCount(this.count.getValue()));
      }

      this.itemPicker.setStack(this.current);
      this.setValue(this.current.copy());
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
