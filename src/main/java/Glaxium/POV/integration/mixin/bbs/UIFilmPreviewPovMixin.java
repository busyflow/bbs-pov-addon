package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIFilmPreview;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = UIFilmPreview.class, remap = false)
public class UIFilmPreviewPovMixin
{
    @Shadow private UIFilmPanel panel;

    @Redirect(
        method = "subMouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/film/replays/UIReplaysEditor;clickViewport(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/utils/Area;)Z"))
    private boolean bbsPov$disableReplayEditorPicking(UIReplaysEditor editor, UIContext context, Area area)
    {
        if (this.panel.getController().getPovMode() == PovCameraMode.POV)
        {
            /* The replay picker sees the actor's head at the POV camera. Route the
             * click exclusively through positions captured from rendered FP hands. */
            return this.panel instanceof UIFilmPanelPovAccess access
                && access.bbsPov$getEditor() != null
                && access.bbsPov$getEditor().pickViewport(context, area);
        }

        return editor.clickViewport(context, area);
    }

}
