package wemppy.bbs_pov;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterActionClipsEvent;
import mchorse.bbs_mod.api.events.RegisterCameraClipsEvent;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import wemppy.bbs_pov.clips.AdvancementToastClip;
import wemppy.bbs_pov.clips.MenuClip;
import wemppy.bbs_pov.clips.MouseTrackerClip;
import wemppy.bbs_pov.clips.ScreenEffectsClip;

public class BBSPovAddon implements BBSAddonMod
{
    @Subscribe
    public void onCameraClips(RegisterCameraClipsEvent event)
    {
        event.factory
            .register(Link.bbs("screen_effects"), ScreenEffectsClip.class, new ClipFactoryData(Icons.MATERIAL, 0xffaa00))
            .register(Link.bbs("menu"), MenuClip.class, new ClipFactoryData(Icons.FONT, Colors.RED))
            .register(Link.bbs("mouse_tracker"), MouseTrackerClip.class, new ClipFactoryData(Icons.POINTER, Colors.ACTIVE))
            .register(Link.bbs("advancement_toast"), AdvancementToastClip.class, new ClipFactoryData(Icons.FAVORITE, Colors.GREEN));
    }

    @Subscribe
    public void onActionClips(RegisterActionClipsEvent event)
    {
        event.factory
            .register(Link.bbs("screen_effects"), ScreenEffectsClip.class, new ClipFactoryData(Icons.MATERIAL, 0xffaa00))
            .register(Link.bbs("menu"), MenuClip.class, new ClipFactoryData(Icons.FONT, Colors.RED))
            .register(Link.bbs("mouse_tracker"), MouseTrackerClip.class, new ClipFactoryData(Icons.POINTER, Colors.ACTIVE))
            .register(Link.bbs("advancement_toast"), AdvancementToastClip.class, new ClipFactoryData(Icons.FAVORITE, Colors.GREEN));
    }
}
