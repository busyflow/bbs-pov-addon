package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.integration.access.bbs.UIReplayPropertiesPovAccess;
import Glaxium.POV.replay.ReplayPovAccess;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayPropertiesPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = UIReplayPropertiesPanel.class, remap = false)
public abstract class UIReplayPropertiesPovMixin implements UIReplayPropertiesPovAccess
{
    @Shadow @Final private UIFilmPanel filmPanel;
    @Shadow public UIElement properties;
    @Shadow public mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox nameTag;
    @Shadow private Replay replay;

    @Shadow
    private void edit(Consumer<Replay> consumer)
    {
    }

    @Unique private UINestedEdit bbsPov$povPickEdit;
    @Unique private UIToggle bbsPov$povHardcoreLook;
    @Unique private UIToggle bbsPov$povCameraShake;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void bbsPov$init(UIFilmPanel filmPanel, CallbackInfo info)
    {
        this.bbsPov$povPickEdit = new UINestedEdit((edit) ->
        {
            if (!edit)
            {
                this.bbsPov$pickBaseForm();
            }
            else
            {
                this.bbsPov$openHandEditor();
            }
        });

        if (this.bbsPov$povPickEdit.pick != null)
        {
            this.bbsPov$povPickEdit.pick.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_PICK_FORM);
        }

        if (this.bbsPov$povPickEdit.edit != null)
        {
            this.bbsPov$povPickEdit.edit.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_EDIT_FORM);
            this.bbsPov$povPickEdit.edit.setEnabled(true);
        }

        this.bbsPov$povHardcoreLook = new UIToggle(IKey.constant("Hardcore look"), (toggle) ->
        {
            this.edit((r) ->
            {
                if (r instanceof ReplayPovAccess access)
                {
                    access.bbsPov$getHardcoreLook().set(toggle.getValue());
                }
            });
        });
        this.bbsPov$povHardcoreLook.tooltip(IKey.constant("Show that replay's actual head bone position in POV Camera mode"));
        this.bbsPov$povHardcoreLook.valueBinding(() ->
        {
            if (this.replay instanceof ReplayPovAccess access)
            {
                this.bbsPov$povHardcoreLook.setValue(access.bbsPov$getHardcoreLook().get());
            }
        });

        this.bbsPov$povCameraShake = new UIToggle(IKey.constant("Camera shake"), (toggle) ->
        {
            this.edit((r) ->
            {
                if (r instanceof ReplayPovAccess access)
                {
                    access.bbsPov$getCameraShake().set(toggle.getValue());
                }
            });
        });
        this.bbsPov$povCameraShake.tooltip(IKey.constant("Show baked Camera Shake for this replay in POV Camera mode"));
        this.bbsPov$povCameraShake.valueBinding(() ->
        {
            if (this.replay instanceof ReplayPovAccess access)
            {
                this.bbsPov$povCameraShake.setValue(access.bbsPov$getCameraShake().get());
            }
        });

        UISection povSection = new UISection(IKey.constant("POV"));
        povSection.fields.add(this.bbsPov$povPickEdit);
        povSection.fields.add(this.bbsPov$povHardcoreLook);
        povSection.fields.add(this.bbsPov$povCameraShake);
        povSection.setExpanded(false);

        this.properties.addAfter(this.nameTag, povSection);
    }

    @Inject(method = "setReplay", at = @At("TAIL"))
    private void bbsPov$setReplay(Replay replay, CallbackInfo info)
    {
        if (this.bbsPov$povPickEdit != null)
        {
            if (replay != null && replay.keyframes instanceof ReplayKeyframesPovAccess access)
            {
                RecordedHandData hand = access.bbsPov$getHand();
                Form baseForm = hand != null && hand.baseForm.get() != null
                    ? hand.baseForm.get()
                    : replay.form.get();
                this.bbsPov$povPickEdit.setForm(baseForm);
            }
            else if (replay != null)
            {
                this.bbsPov$povPickEdit.setForm(replay.form.get());
            }
            else
            {
                this.bbsPov$povPickEdit.setForm(null);
            }

            if (this.bbsPov$povPickEdit.edit != null)
            {
                this.bbsPov$povPickEdit.edit.setEnabled(true);
            }
        }
    }

    @Override
    public void bbsPov$setPovMode(boolean povActive)
    {
    }

    @Unique
    private void bbsPov$pickBaseForm()
    {
        Replay currentReplay = this.replay != null
            ? this.replay
            : (this.filmPanel.replayEditor != null ? this.filmPanel.replayEditor.getReplay() : null);

        if (currentReplay == null)
        {
            return;
        }

        if (!(currentReplay.keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return;
        }

        RecordedHandData hand = access.bbsPov$getHand();

        if (hand == null)
        {
            return;
        }

        Form current = hand.baseForm.get();

        if (current == null)
        {
            current = currentReplay.form.get();
        }

        UIElement parent = this.filmPanel;

        if (this.filmPanel.getRoot() != null)
        {
            parent = this.filmPanel.getParentContainer();
        }

        UIFormPalette palette = UIFormPalette.open(parent, false, current, (picked) ->
        {
            Form copy = picked == null ? null : FormUtils.copy(picked);

            hand.baseForm.set(copy);

            if (this.bbsPov$povPickEdit != null)
            {
                this.bbsPov$povPickEdit.setForm(copy);

                if (this.bbsPov$povPickEdit.edit != null)
                {
                    this.bbsPov$povPickEdit.edit.setEnabled(true);
                }
            }

            if (this.filmPanel instanceof UIFilmPanelPovAccess povAccess)
            {
                UIPovEditor editor = povAccess.bbsPov$getEditor();

                if (editor != null)
                {
                    editor.reloadHandModel();
                }
            }

            if (this.filmPanel.replayEditor != null && this.filmPanel.replayEditor.replaysList != null)
            {
                this.filmPanel.replayEditor.replaysList.replays.update();
                this.filmPanel.replayEditor.replaysList.setBodyPartsReplay(currentReplay, new mchorse.bbs_mod.film.replays.tracks.TimelineBodyPartSelection());
            }
        });

        if (palette != null)
        {
            palette.updatable();
        }
    }

    @Unique
    private void bbsPov$openHandEditor()
    {
        Replay currentReplay = this.replay != null
            ? this.replay
            : (this.filmPanel.replayEditor != null ? this.filmPanel.replayEditor.getReplay() : null);
        RecordedHandData hand = null;

        if (currentReplay != null && currentReplay.keyframes instanceof ReplayKeyframesPovAccess access)
        {
            hand = access.bbsPov$getHand();
        }

        Glaxium.POV.editor.UIPovHandEditor.open(this.filmPanel, currentReplay, hand);
    }
}
