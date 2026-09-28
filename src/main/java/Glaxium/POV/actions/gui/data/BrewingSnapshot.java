package Glaxium.POV.actions.gui.data;

public final class BrewingSnapshot {
   public final float progress;
   public final float fuel;
   public final boolean bubbles;

   public BrewingSnapshot(float progress, float fuel, boolean bubbles) {
      this.progress = progress;
      this.fuel = fuel;
      this.bubbles = bubbles;
   }
}
