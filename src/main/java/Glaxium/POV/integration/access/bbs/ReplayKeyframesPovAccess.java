package Glaxium.POV.integration.access.bbs;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;

public interface ReplayKeyframesPovAccess {
   RecordedHudData bbsPov$getHud();

   RecordedHandData bbsPov$getHand();

   RecordedPovActions bbsPov$getActions();
}
