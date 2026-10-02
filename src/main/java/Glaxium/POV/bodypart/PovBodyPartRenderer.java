package Glaxium.POV.bodypart;

import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;

/** Bridge implemented by BBS's ModelFormRenderer mixin so POV playback can
 * render the configured bodyparts after both first-person arms are available. */
public interface PovBodyPartRenderer
{
    void bbsPov$renderBodyParts(int light, StencilMap stencilMap);
}
