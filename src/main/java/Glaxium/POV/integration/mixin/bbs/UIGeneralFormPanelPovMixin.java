package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = UIGeneralFormPanel.class, remap = false)
public abstract class UIGeneralFormPanelPovMixin extends mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel
{
    public UIGeneralFormPanelPovMixin(mchorse.bbs_mod.ui.forms.editors.forms.UIForm editor)
    {
        super(editor);
    }

    @Shadow public UIPropTransform transform;
    @Shadow public UIToggle hitbox;
    @Shadow public UITrackpad hp;

    @Inject(method = "startEdit", at = @At("TAIL"))
    private void bbsPov$hideSectionsInPov(Form form, CallbackInfo info)
    {
        boolean isHandEditMode = UIPovHandEditor.isActive();

        if (!isHandEditMode)
        {
            mchorse.bbs_mod.ui.film.UIFilmPanel panel = PovReplaySettings.getFilmPanel();
            if (panel instanceof Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess access && access.bbsPov$isPovActive())
            {
                Glaxium.POV.editor.UIPovEditor povEditor = access.bbsPov$getEditor();
                if (povEditor != null && povEditor.getSection() == Glaxium.POV.editor.UIPovEditor.Section.HAND && form == povEditor.getHandEditorForm())
                {
                    isHandEditMode = true;
                }
            }
        }

        if (this.options != null)
        {
            UISection transformSection = this.transform != null ? this.transform.getParent(UISection.class) : null;
            UISection hitboxSection = this.hitbox != null ? this.hitbox.getParent(UISection.class) : null;
            UISection movementSection = this.hp != null ? this.hp.getParent(UISection.class) : null;

            if (transformSection != null)
            {
                transformSection.setVisible(!isHandEditMode);
            }
            if (hitboxSection != null)
            {
                hitboxSection.setVisible(!isHandEditMode);
            }
            if (movementSection != null)
            {
                movementSection.setVisible(!isHandEditMode);
            }

            this.options.resize();
        }
    }

    @Inject(method = "openTrackFilter", at = @At("HEAD"), cancellable = true)
    private void bbsPov$openTrackFilter(CallbackInfo info)
    {
        if (this.form == null)
        {
            return;
        }

        boolean isHandEditor = UIPovHandEditor.isActive();
        mchorse.bbs_mod.ui.film.UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        Glaxium.POV.editor.UIPovEditor povEditor = null;
        if (panel instanceof Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess access)
        {
            povEditor = access.bbsPov$getEditor();
        }

        boolean isPovCamera = panel != null && panel.getController() != null && panel.getController().getPovMode() == Glaxium.POV.camera.PovCameraMode.POV;
        if (isHandEditor || (isPovCamera && povEditor != null && (this.form == povEditor.getHandEditorForm() || povEditor.isPoseGizmoSection())))
        {
            info.cancel();
            openFormTrackFilter(this.form, povEditor);
            return;
        }
    }

    private void openFormTrackFilter(Form form, Glaxium.POV.editor.UIPovEditor povEditor)
    {
        java.util.Set<String> disabled = form.disabledTracks.get();
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        java.util.Map<String, Integer> keyToColor = new java.util.HashMap<>();

        for (mchorse.bbs_mod.film.replays.tracks.TrackDescriptor track : mchorse.bbs_mod.film.replays.tracks.TrackCatalog.of(form))
        {
            keys.add(track.filterKey());
            keyToColor.put(track.filterKey(), track.color());
        }

        mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel panel =
            new mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel(disabled, keys, keyToColor);

        mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);
        panel.onClose(e ->
        {
            form.disabledTracks.set(disabled);
            if (povEditor != null)
            {
                povEditor.refreshSheets(false);
            }
        });
    }
}
