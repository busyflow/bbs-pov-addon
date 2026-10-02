package Glaxium.POV.replay;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

/** Persistent BBS-POV settings attached to every BBS replay. */
public interface ReplayPovAccess
{
    ValueBoolean bbsPov$getOverlayEnabled();

    ValueBoolean bbsPov$getHardcoreLook();

    ValueBoolean bbsPov$getCameraShake();
}
