package Glaxium.POV.replay;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

public interface ReplayPovAccess {
   ValueBoolean bbsPov$getOverlayEnabled();

   ValueBoolean bbsPov$getHardcoreLook();

   ValueBoolean bbsPov$getCameraShake();
}
