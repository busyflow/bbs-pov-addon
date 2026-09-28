package Glaxium.POV.actions.gui.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;

public final class GuiSnapshot {
   public final String guiType;
   public final float cursorTx;
   public final float cursorTy;
   public final boolean cursorVisible;
   public final ItemStack cursorItem;
   public final boolean dragging;
   public final String dragEncoded;
   public final Map<String, ItemStack> slots;
   public final List<String> dragKeys;

   public GuiSnapshot(
      String guiType,
      float cursorTx,
      float cursorTy,
      boolean cursorVisible,
      ItemStack cursorItem,
      boolean dragging,
      String dragEncoded,
      Map<String, ItemStack> slots,
      List<String> dragKeys
   ) {
      this.guiType = guiType;
      this.cursorTx = cursorTx;
      this.cursorTy = cursorTy;
      this.cursorVisible = cursorVisible;
      this.cursorItem = cursorItem == null ? ItemStack.EMPTY : cursorItem;
      this.dragging = dragging;
      this.dragEncoded = dragEncoded == null ? "" : dragEncoded;
      this.slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
      this.dragKeys = dragKeys == null ? List.of() : List.copyOf(dragKeys);
   }
}
