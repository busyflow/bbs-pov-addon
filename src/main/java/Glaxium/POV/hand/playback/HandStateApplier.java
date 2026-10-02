package Glaxium.POV.hand.playback;

import Glaxium.POV.bodypart.PovBodyPartPlayback;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.HandState;
import Glaxium.POV.hand.RecordedHandData;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

/** Applies sampled hand state onto the first-person ModelForm. */
public final class HandStateApplier
{
    private HandStateApplier()
    {
    }

    /** Empty POV identity tracks inherit the selected replay actor or Base POV Form. */
    public static void inheritReplayModel(
        RecordedHandData data,
        HandState state,
        Replay replay,
        UIFilmPanel panel)
    {
        ModelForm baseModelForm = null;

        if (data != null && data.baseForm.get() != null)
        {
            Form root = FormUtils.getRoot(data.baseForm.get());
            if (root instanceof ModelForm mf && mf.model.get() != null && !mf.model.get().isBlank())
            {
                baseModelForm = mf;
            }
        }

        if (baseModelForm == null)
        {
            IEntity entity = null;

            if (panel != null)
            {
                Film film = (Film) panel.getData();
                int selector = PovCameraClips.indexOfReplay(film, replay);

                if (selector >= 0)
                {
                    entity = panel.getController().getEntities().get(selector);
                }
            }

            Form source = entity == null ? (replay != null ? replay.form.get() : null) : entity.getForm();
            Form root = source == null ? null : FormUtils.getRoot(source);

            if (root instanceof ModelForm mf && mf.model.get() != null && !mf.model.get().isBlank())
            {
                baseModelForm = mf;
            }
        }

        if (baseModelForm == null)
        {
            if (state.model == null || state.model.isBlank())
            {
                state.model = RecordedHandData.DEFAULT_MODEL;
            }
            return;
        }

        if (state.model == null || state.model.isBlank())
        {
            String model = baseModelForm.model.get();

            if (model != null && !model.isBlank())
            {
                state.model = model;
            }
            else
            {
                state.model = RecordedHandData.DEFAULT_MODEL;
            }
        }

        if (state.texture == null)
        {
            state.texture = state.model.equals(baseModelForm.model.get())
                ? baseModelForm.texture.get()
                : null;
        }

        if (state.color == null && baseModelForm.color.get() != null)
        {
            state.color = baseModelForm.color.get().copy();
        }

        if (state.colorOverlay == null && baseModelForm.overlayColor.get() != null)
        {
            state.colorOverlay = baseModelForm.overlayColor.get().copy();
        }

        if (baseModelForm.pose.get() != null && state.pose.transforms.isEmpty())
        {
            state.pose.copy(baseModelForm.pose.get());
        }
    }

    public static void applyForm(
        ModelForm form,
        HandState state,
        RecordedHandData data,
        float tick)
    {
        RecordedHandData.ensureOverlays(form);
        form.visible.set(true);
        form.model.set(state.model == null || state.model.isBlank() ? RecordedHandData.DEFAULT_MODEL : state.model);
        /* ModelFormRenderer falls back to ModelInstance.getTexture() for null. */
        form.texture.set(state.texture);
        form.color.set(state.color != null ? state.color.copy() : Color.white());
        form.overlayColor.set(state.colorOverlay != null ? state.colorOverlay.copy() : new Color(1F, 1F, 1F, 0F));

        String rightBone = "right_arm";
        String leftBone = "left_arm";
        FormRenderer<?> renderer = FormUtilsClient.getRenderer(form);

        if (renderer instanceof ModelFormRenderer modelRenderer)
        {
            ModelInstance model = modelRenderer.getModel();

            if (model != null)
            {
                rightBone = getBone(model.getFpMain(), rightBone);
                leftBone = getBone(model.getFpOffhand(), leftBone);
            }
        }

        Pose pose = state.pose.copy();
        if (data != null && !data.rightPose.isEmpty())
        {
            pose.transforms.put(rightBone, copyPose(state.rightPose));
        }
        if (data != null && !data.leftPose.isEmpty())
        {
            pose.transforms.put(leftBone, copyPose(state.leftPose));
        }
        form.pose.set(pose);

        if (!Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            data.applyPoseTracks(form, tick);

            if (data != null && data.baseForm.get() != null)
            {
                Form root = FormUtils.getRoot(data.baseForm.get());
                if (root instanceof ModelForm mf)
                {
                    /* BodyPartManager.copy(BaseValueGroup) only recreates list
                     * entries; it does not copy BodyPart data or the nested form.
                     * That left playback with correctly-sized, permanently empty
                     * bodypart slots whose getForm() was null.  Deserialize the
                     * manager data to perform the real deep copy. */
                    PovBodyPartPlayback.sync(form, mf);
                }
            }

            data.applyBodyPartTracks(form, tick);
            PovBodyPartPlayback.syncHoverFrame(form);
        }
        else
        {
            Form preview = Glaxium.POV.editor.UIPovHandEditor.getActive() != null
                ? Glaxium.POV.editor.UIPovHandEditor.getActive().getPreviewForm()
                : null;

            if (preview != null)
            {
                Form root = FormUtils.getRoot(preview);

                if (root instanceof ModelForm mf)
                {
                    PovBodyPartPlayback.sync(form, mf);
                }
            }
        }
    }

    private static String getBone(ArmorSlot slot, String fallback)
    {
        return slot == null || slot.group == null || slot.group.isBlank() ? fallback : slot.group;
    }

    public static HandState createDefaultHandEditorState(RecordedHandData data, Replay replay)
    {
        HandState state = new HandState();
        state.visible = true;
        state.rightHandVisible = true;
        state.leftHandVisible = true;

        String model = null;
        Link texture = null;

        Form activeForm = Glaxium.POV.editor.UIPovHandEditor.getActive() != null
            ? Glaxium.POV.editor.UIPovHandEditor.getActive().getPreviewForm()
            : null;

        if (activeForm == null && data != null)
        {
            activeForm = data.baseForm.get();
        }

        if (activeForm != null)
        {
            Form root = FormUtils.getRoot(activeForm);

            if (root instanceof ModelForm mf && mf.model.get() != null && !mf.model.get().isBlank())
            {
                model = mf.model.get();
                texture = mf.texture.get();

                if (mf.color.get() != null)
                {
                    state.color = mf.color.get().copy();
                }

                if (mf.overlayColor.get() != null)
                {
                    state.colorOverlay = mf.overlayColor.get().copy();
                }

                if (mf.pose.get() != null)
                {
                    state.pose.copy(mf.pose.get());
                }
            }
        }

        if ((model == null || model.isBlank()) && replay != null && replay.form.get() != null)
        {
            Form root = FormUtils.getRoot(replay.form.get());

            if (root instanceof ModelForm mf && mf.model.get() != null && !mf.model.get().isBlank())
            {
                model = mf.model.get();
                texture = mf.texture.get();

                if (state.color == null && mf.color.get() != null)
                {
                    state.color = mf.color.get().copy();
                }

                if (state.colorOverlay == null && mf.overlayColor.get() != null)
                {
                    state.colorOverlay = mf.overlayColor.get().copy();
                }
            }
        }

        if (model == null || model.isBlank())
        {
            model = RecordedHandData.DEFAULT_MODEL;
            texture = RecordedHandData.DEFAULT_TEXTURE;
        }

        state.model = model;
        state.texture = texture;

        return state;
    }

    private static PoseTransform copyPose(PoseTransform source)
    {
        PoseTransform copy = new PoseTransform();
        copy.copy(source);
        return copy;
    }
}
