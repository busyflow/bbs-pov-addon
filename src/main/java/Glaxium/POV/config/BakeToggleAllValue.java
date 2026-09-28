package Glaxium.POV.config;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

public class BakeToggleAllValue extends ValueBoolean {
   public BakeToggleAllValue(String id) {
      super(id, true);
   }

   public Boolean get() {
      return PovSettings.areAllBakeEnabled();
   }

   public void set(Boolean value) {
      super.set(value);
      PovSettings.setAllBake(value != null && value);
   }
}
