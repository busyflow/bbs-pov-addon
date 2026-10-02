package Glaxium.POV.replay;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;

/**
 * Java-only view of the three POV systems on a replay. Not a serialized node;
 * ReplayKeyframes still stores HUD, Hand, and Actions separately.
 */
public final class PovReplayData
{
    public final RecordedHudData hud;
    public final RecordedHandData hand;
    public final RecordedPovActions actions;

    public PovReplayData(RecordedHudData hud, RecordedHandData hand, RecordedPovActions actions)
    {
        this.hud = hud;
        this.hand = hand;
        this.actions = actions;
    }

    public static PovReplayData of(ReplayKeyframesPovAccess access)
    {
        return new PovReplayData(access.bbsPov$getHud(), access.bbsPov$getHand(), access.bbsPov$getActions());
    }
}
