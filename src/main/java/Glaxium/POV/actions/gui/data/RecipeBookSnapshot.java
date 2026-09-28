package Glaxium.POV.actions.gui.data;

public final class RecipeBookSnapshot {
   public final boolean open;
   public final String search;
   public final boolean showing;
   public final int category;
   public final String selected;
   public final int page;
   public final boolean buttonSelected;
   public final boolean searchFocused;
   public final int searchSelStart;
   public final int searchSelEnd;

   public RecipeBookSnapshot(
      boolean open,
      String search,
      boolean showing,
      int category,
      String selected,
      int page,
      boolean buttonSelected,
      boolean searchFocused,
      int searchSelStart,
      int searchSelEnd
   ) {
      this.open = open;
      this.search = search == null ? "" : search;
      this.showing = showing;
      this.category = Math.max(0, category);
      this.selected = selected == null ? "" : selected;
      this.page = Math.max(0, page);
      this.buttonSelected = buttonSelected;
      this.searchFocused = searchFocused;
      this.searchSelStart = Math.max(0, searchSelStart);
      this.searchSelEnd = Math.max(0, searchSelEnd);
   }
}
