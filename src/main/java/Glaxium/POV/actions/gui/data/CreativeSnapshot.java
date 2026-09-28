package Glaxium.POV.actions.gui.data;

public final class CreativeSnapshot {
   public final int tab;
   public final float scroll;
   public final String search;
   public final int page;
   public final boolean inventoryTab;
   public final int row;
   public final boolean searchFocused;
   public final int searchSelStart;
   public final int searchSelEnd;

   public CreativeSnapshot(
      int tab, float scroll, String search, int page, boolean inventoryTab, int row, boolean searchFocused, int searchSelStart, int searchSelEnd
   ) {
      this.tab = Math.max(0, tab);
      this.scroll = Math.max(0.0F, Math.min(1.0F, scroll));
      this.search = search == null ? "" : search;
      this.page = Math.max(0, page);
      this.inventoryTab = inventoryTab;
      this.row = Math.max(0, row);
      this.searchFocused = searchFocused;
      this.searchSelStart = Math.max(0, searchSelStart);
      this.searchSelEnd = Math.max(0, searchSelEnd);
   }
}
