package wemppy.bbs_pov.client;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFrameOverlaysEvent;
import wemppy.bbs_pov.client.clips.ui.UIAdvancementToastClip;
import wemppy.bbs_pov.client.clips.ui.UIMenuClip;
import wemppy.bbs_pov.client.clips.ui.UIMouseTrackerClip;
import wemppy.bbs_pov.client.clips.ui.UIScreenEffectsClip;
import wemppy.bbs_pov.client.render.AdvancementToastRenderer;
import wemppy.bbs_pov.client.render.DeathScreenOverlayRenderer;
import wemppy.bbs_pov.client.render.MouseCursorRenderer;
import wemppy.bbs_pov.client.render.PovParticleEmitter;
import wemppy.bbs_pov.client.render.TotemFloatingItemRenderer;
import wemppy.bbs_pov.clips.AdvancementToastClip;
import wemppy.bbs_pov.clips.MenuClip;
import wemppy.bbs_pov.clips.MouseTrackerClip;
import wemppy.bbs_pov.clips.ScreenEffectsClip;

public class BBSPovClientAddon implements BBSAddonMod
{
    @Subscribe
    public void onClipPanels(RegisterClipPanelsEvent event)
    {
        event.register(ScreenEffectsClip.class, UIScreenEffectsClip::new);
        event.register(MenuClip.class, UIMenuClip::new);
        event.register(MouseTrackerClip.class, UIMouseTrackerClip::new);
        event.register(AdvancementToastClip.class, UIAdvancementToastClip::new);
    }

    @Subscribe
    public void onFrameOverlays(RegisterFrameOverlaysEvent event)
    {
        event.register(TotemFloatingItemRenderer::render);
        event.register(DeathScreenOverlayRenderer::render);
        event.register(MouseCursorRenderer::render);
        event.register(AdvancementToastRenderer::render);
        event.register(PovParticleEmitter::render);
    }
}
