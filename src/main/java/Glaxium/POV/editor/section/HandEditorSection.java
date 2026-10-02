package Glaxium.POV.editor.section;

import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.replays.tracks.TrackCatalog;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hand keyframe sheets, bone picking, model resolution, and gizmo matrix. */
public final class HandEditorSection implements PovEditorSection
{
    public static final HandEditorSection INSTANCE = new HandEditorSection();

    private HandEditorSection()
    {
    }

    @Override
    public void fillSheets(UIPovEditor editor, boolean resetView)
    {
        Replay replay = editor.getReplay();
        if (replay == null)
        {
            return;
        }

        ModelForm handForm = editor.getHandEditorForm();

        if (replay.keyframes instanceof ReplayKeyframesPovAccess access)
        {
            RecordedHandData hand = access.bbsPov$getHand();

            if (hand != null)
            {
                RecordedHandData.ensureOverlays(handForm);
                handForm.model.set(currentModel(editor, hand));
                handForm.texture.set(currentTexture(editor, hand));
                handForm.boneTracks.set(true);

                ModelForm baseForm = baseModelForm(editor, hand);
                if (!hand.color.isEmpty())
                {
                    handForm.color.set(hand.color.interpolate(editor.getReplayTick(), Color.white()).copy());
                }
                else if (baseForm != null && baseForm.color.get() != null)
                {
                    handForm.color.set(baseForm.color.get().copy());
                }
                else
                {
                    handForm.color.set(Color.white());
                }

                if (!hand.colorOverlay.isEmpty())
                {
                    handForm.overlayColor.set(hand.colorOverlay.interpolate(editor.getReplayTick(), new Color(1F, 1F, 1F, 0F)).copy());
                }
                else if (baseForm != null && baseForm.overlayColor.get() != null)
                {
                    handForm.overlayColor.set(baseForm.overlayColor.get().copy());
                }
                else
                {
                    handForm.overlayColor.set(new Color(1F, 1F, 1F, 0F));
                }

                if (baseForm != null && baseForm.pose.get() != null)
                {
                    handForm.pose.set(baseForm.pose.get().copy());
                }
                else
                {
                    handForm.pose.set(new Pose());
                }

                UIKeyframeSheet visibleSheet = editor.addSheetWithColor(
                    "Visible",
                    hand.visible,
                    Icons.VISIBLE,
                    UIReplaysEditor.getColor("visible"),
                    handForm.visible,
                    () -> hand.visible.interpolate(editor.getReplayTick(), true));
                visibleSheet.form(handForm);

                UIKeyframeSheet modelSheet = editor.addSheetWithColor(
                    "Model",
                    hand.model,
                    Icons.MORPH,
                    UIReplaysEditor.getColor("model"),
                    handForm.model,
                    () -> currentModel(editor, hand));
                modelSheet.form(handForm);

                UIKeyframeSheet textureSheet = editor.addSheetWithColor(
                    "Texture",
                    hand.texture,
                    Icons.IMAGE,
                    UIReplaysEditor.getColor("texture"),
                    handForm.texture,
                    () -> currentTexture(editor, hand));
                textureSheet.form(handForm);

                UIKeyframeSheet colorSheet = editor.addSheetWithColor(
                    "Color",
                    hand.color,
                    Icons.COLOR,
                    UIReplaysEditor.getColor("color"),
                    handForm.color,
                    () -> currentColor(editor, hand));
                colorSheet.form(handForm);

                UIKeyframeSheet colorOverlaySheet = editor.createSheetWithColor(
                    "Color Overlay",
                    hand.colorOverlay,
                    Icons.COLOR,
                    UIReplaysEditor.getColor("color_overlay"),
                    handForm.overlayColor,
                    () -> TrackCatalog.opaqueOverlaySeed(currentColorOverlay(editor, hand)));
                colorOverlaySheet.form(handForm);

                UIKeyframeSheet cameraOffsetSheet = editor.addSheetWithColor(
                    "Camera Offset",
                    hand.cameraOffset,
                    Icons.LAYOUT,
                    UIReplaysEditor.getColor("transform"),
                    () -> hand.cameraOffset.interpolate(editor.getReplayTick(), new Transform()).copy());
                cameraOffsetSheet.form(handForm);

                Pose defaultPose = handForm.pose.get() != null
                    ? handForm.pose.get()
                    : new Pose();

                UIKeyframeSheet poseSheet = editor.createSheetWithColor(
                    "Pose",
                    hand.pose,
                    Icons.POSE,
                    UIReplaysEditor.getColor("pose"),
                    handForm.pose,
                    () -> hand.pose.interpolate(editor.getReplayTick(), defaultPose).copy());
                poseSheet.form(handForm);

                if (BBSSettings.recordingOverlays.get())
                {
                    String overlayKey = "pose_overlay";
                    KeyframeChannel<Pose> overlayChannel = hand.getOrCreatePoseOverlay(handForm, overlayKey);
                    if (overlayChannel != null)
                    {
                        UIKeyframeSheet overlaySheet = new UIKeyframeSheet(
                            overlayKey,
                            IKey.constant(overlayKey),
                            UIReplaysEditor.getColor(overlayKey),
                            overlayChannel,
                            handForm.poseOverlay);
                        overlaySheet.icon(Icons.POSE).form(handForm);
                        overlaySheet.seed(() -> overlayChannel.interpolate(editor.getReplayTick(), new Pose()).copy());
                        /* [PORTING NOTE: Collapse pose overlay tracks under the Pose folder in DopeSheet] */
                        overlaySheet.setParent(poseSheet);
                        editor.addPendingSheet(overlaySheet);
                    }

                    int additional = BBSSettings.recordingPoseOverlays.get();
                    for (int k = 0; k < additional; k++)
                    {
                        String addKey = "pose_overlay" + k;
                        KeyframeChannel<Pose> addChannel = hand.getOrCreatePoseOverlay(handForm, addKey);
                        BaseValueBasic property = FormUtils.getProperty(handForm, addKey);
                        if (addChannel != null && property != null)
                        {
                            UIKeyframeSheet addSheet = new UIKeyframeSheet(
                                addKey,
                                IKey.constant(addKey),
                                UIReplaysEditor.getColor(addKey),
                                addChannel,
                                property);
                            addSheet.icon(Icons.POSE).form(handForm);
                            addSheet.seed(() -> addChannel.interpolate(editor.getReplayTick(), new Pose()).copy());
                            /* [PORTING NOTE: Collapse additional pose overlay tracks under Pose] */
                            addSheet.setParent(poseSheet);
                            editor.addPendingSheet(addSheet);
                        }
                    }
                }

                int color = 0;

                String currentHandModel = currentModel(editor, hand);
                handForm.model.set(currentHandModel);
                ModelInstance model = ModelFormRenderer.getModel(handForm);
                if (model == null || model.getModel() == null)
                {
                    if (baseForm != null)
                    {
                        model = ModelFormRenderer.getModel(baseForm);
                    }
                }
                if (model == null || model.getModel() == null)
                {
                    if (BBSModClient.getModels() != null)
                    {
                        model = BBSModClient.getModels().getModel(currentHandModel);
                        if (model == null || model.getModel() == null)
                        {
                            model = BBSModClient.getModels().getModel(RecordedHandData.DEFAULT_MODEL);
                        }
                    }
                }

                HandBoneUtils.HandBones handBones = HandBoneUtils.collect(model);
                Map<String, UIKeyframeSheet> handBoneSheets = new LinkedHashMap<>();

                for (Map.Entry<String, Integer> entry : handBones.depths().entrySet())
                {
                    String bone = entry.getKey();
                    KeyframeChannel<PoseTransform> channel = hand.getOrCreatePoseTrack(bone, handBones);
                    String sheetId = TrackId.bone("", bone).toKey();
                    UIKeyframeSheet boneSheet = createBoneSheet(
                        editor,
                        sheetId,
                        bone,
                        channel,
                        color++);

                    if (model != null && model.getModel() != null)
                    {
                        String parentBone = model.getModel().getParentGroupKey(bone);
                        UIKeyframeSheet parentSheet = handBoneSheets.get(parentBone);
                        boneSheet.setParent(parentSheet != null ? parentSheet : poseSheet);
                    }
                    else
                    {
                        boneSheet.setParent(poseSheet);
                    }
                    handBoneSheets.put(bone, boneSheet);
                }

                UIKeyframeSheet itemPoseSheet = editor.createSheetWithColor(
                    "Item Pose",
                    hand.itemPose,
                    Icons.BLOCK,
                    UIReplaysEditor.getColor("pose"),
                    handForm.pose,
                    () -> hand.itemPose.interpolate(editor.getReplayTick(), defaultPose).copy());
                itemPoseSheet.form(handForm);

                editor.addSheetWithColor(
                    "World Interaction",
                    hand.worldInteraction,
                    Icons.BLOCK,
                    UIReplaysEditor.getColor("world_interaction"),
                    () -> hand.worldInteraction.interpolate(editor.getReplayTick(), false));
                editor.addSheetWithColor(
                    "Replay Interaction",
                    hand.replayInteraction,
                    Icons.FILM,
                    UIReplaysEditor.getColor("replay_interaction"),
                    () -> hand.replayInteraction.interpolate(editor.getReplayTick(), false));

                editor.addSheetWithColor(
                    "Right Hand Visible",
                    hand.rightHandVisible,
                    Icons.VISIBLE,
                    UIReplaysEditor.getColor("visible"),
                    () -> hand.rightHandVisible.interpolate(editor.getReplayTick(), true));
                editor.addSheetWithColor(
                    "Left Hand Visible",
                    hand.leftHandVisible,
                    Icons.VISIBLE,
                    UIReplaysEditor.getColor("visible"),
                    () -> hand.leftHandVisible.interpolate(editor.getReplayTick(), false));

                color = editor.addSheet("Off Hand Item", replay.keyframes.offHand, Icons.BLOCK, color);
                color = editor.addSheet("Right Swing", hand.rightSwingProgress, Icons.MAIN_HANDLE, color);
                color = editor.addSheet("Left Swing", hand.leftSwingProgress, Icons.LEFT_HANDLE, color);
                color = editor.addSheet("Main Equip", hand.mainEquipProgress, Icons.MAIN_HANDLE, color);
                color = editor.addSheet("Offhand Equip", hand.offEquipProgress, Icons.LEFT_HANDLE, color);
                color = editor.addSheet("Active Use Hand", hand.activeHand, Icons.POINTER, color);
                color = editor.addSheet("Active Use Item", hand.activeItem, Icons.BLOCK, color);
                color = editor.addSheet("Show Particles", hand.showUseParticles, Icons.BUBBLE, color);
                color = editor.addSheet("Use Time", hand.useTime, Icons.TIME, color);
                color = editor.addSheet("Bob Phase", hand.bobPhase, Icons.CURVES, color);
                color = editor.addSheet("Bob Strength", hand.bobStrength, Icons.CURVES, color);
                color = editor.addSheet("Render Yaw", hand.renderYaw, Icons.SPHERE, color);
                color = editor.addSheet("Render Pitch", hand.renderPitch, Icons.SPHERE, color);
                editor.addSheet("Left-handed Main Arm", hand.mainArm, Icons.LIMB, color);
            }
        }
    }

    /** Select only a bone belonging to a hand rendered in the current POV frame. */
    public boolean pick(UIPovEditor editor, UIContext context, Area viewport)
    {
        if (!editor.isVisible() || !editor.isHandSection() || editor.getReplay() == null)
        {
            return false;
        }

        if (!(editor.getReplay().keyframes instanceof ReplayKeyframesPovAccess access))
        {
            return false;
        }

        RecordedHandData hand = access.bbsPov$getHand();

        if (hand == null)
        {
            return false;
        }

        String itemBone = context.mouseButton == 0 && !hand.itemPose.isEmpty()
            ? PovHandPicking.pickItem(context, viewport)
            : null;

        if (itemBone != null)
        {
            editor.selectClosestKeyframe(hand.itemPose);

            if (editor.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                poseFactory.poseEditor.selectBone(itemBone);
            }

            return true;
        }

        String bone = context.mouseButton == 0 ? PovHandPicking.getPickedBone() : null;

        if (bone == null)
        {
            return false;
        }

        KeyframeChannel<Pose> activeOverlay = null;
        int additional = BBSSettings.recordingPoseOverlays.get();
        for (int k = additional - 1; k >= 0; k--)
        {
            KeyframeChannel<Pose> add = (KeyframeChannel<Pose>) (Object) hand.poseTracks.get(TrackId.property("", "pose_overlay" + k));
            if (add != null && !add.isEmpty())
            {
                activeOverlay = add;
                break;
            }
        }
        if (activeOverlay == null)
        {
            KeyframeChannel<Pose> overlay = (KeyframeChannel<Pose>) (Object) hand.poseTracks.get(TrackId.property("", "pose_overlay"));
            if (overlay != null && !overlay.isEmpty())
            {
                activeOverlay = overlay;
            }
        }

        KeyframeChannel<Pose> targetPoseChannel = activeOverlay != null ? activeOverlay : hand.pose;
        editor.selectClosestKeyframe(targetPoseChannel);

        if (editor.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
        {
            poseFactory.poseEditor.selectBone(bone);
        }

        return true;
    }

    public void reloadModel(UIPovEditor editor)
    {
        if (editor.getReplay() != null && editor.getReplay().keyframes instanceof ReplayKeyframesPovAccess access)
        {
            RecordedHandData hand = access.bbsPov$getHand();
            if (hand != null)
            {
                editor.getHandEditorForm().model.set(currentModel(editor, hand));
                editor.getHandEditorForm().texture.set(currentTexture(editor, hand));
                ModelForm baseForm = baseModelForm(editor, hand);
                if (baseForm != null && baseForm.pose.get() != null)
                {
                    editor.getHandEditorForm().pose.set(baseForm.pose.get().copy());
                }
            }
        }

        if (editor.isHandSection())
        {
            editor.refreshSheets(false);
        }
    }

    public String currentModel(UIPovEditor editor, RecordedHandData hand)
    {
        String model = hand.model.interpolate(editor.getReplayTick(), null);

        if (model == null || model.isBlank())
        {
            ModelForm baseForm = baseModelForm(editor, hand);

            if (baseForm != null)
            {
                model = baseForm.model.get();
            }
        }

        return model == null || model.isBlank() ? RecordedHandData.DEFAULT_MODEL : model;
    }

    public Link currentTexture(UIPovEditor editor, RecordedHandData hand)
    {
        Link authored = hand.texture.interpolate(editor.getReplayTick(), null);

        if (authored != null)
        {
            return authored;
        }

        ModelForm baseForm = baseModelForm(editor, hand);
        String selectedModel = currentModel(editor, hand);

        if (baseForm != null && selectedModel.equals(baseForm.model.get()))
        {
            Link baseTexture = baseForm.texture.get();

            if (baseTexture != null)
            {
                return baseTexture;
            }
        }

        /* An un-authored texture means the selected model's own texture. Keep the
         * editor form on the sampled model so the Model key and Texture key can
         * be added in either order at the same cursor position. */
        editor.getHandEditorForm().model.set(currentModel(editor, hand));
        ModelInstance model = ModelFormRenderer.getModel(editor.getHandEditorForm());
        Link texture = model == null ? null : model.getTexture();

        return texture == null ? RecordedHandData.DEFAULT_TEXTURE : texture;
    }

    public Color currentColor(UIPovEditor editor, RecordedHandData hand)
    {
        if (hand != null && !hand.color.isEmpty())
        {
            Color authored = hand.color.interpolate(editor.getReplayTick(), null);
            if (authored != null)
            {
                return authored.copy();
            }
        }

        ModelForm baseForm = baseModelForm(editor, hand);
        if (baseForm != null && baseForm.color.get() != null)
        {
            return baseForm.color.get().copy();
        }

        return Color.white();
    }

    public Color currentColorOverlay(UIPovEditor editor, RecordedHandData hand)
    {
        if (hand != null && !hand.colorOverlay.isEmpty())
        {
            Color authored = hand.colorOverlay.interpolate(editor.getReplayTick(), null);
            if (authored != null)
            {
                return authored.copy();
            }
        }

        ModelForm baseForm = baseModelForm(editor, hand);
        if (baseForm != null && baseForm.overlayColor.get() != null)
        {
            return baseForm.overlayColor.get().copy();
        }

        return new Color(1F, 1F, 1F, 0F);
    }

    public ModelForm baseModelForm(UIPovEditor editor, RecordedHandData hand)
    {
        if (hand != null && hand.baseForm.get() != null)
        {
            Form root = FormUtils.getRoot(hand.baseForm.get());
            if (root instanceof ModelForm modelForm && modelForm.model.get() != null && !modelForm.model.get().isBlank())
            {
                return modelForm;
            }
        }
        return replayModelForm(editor);
    }

    private ModelForm replayModelForm(UIPovEditor editor)
    {
        IEntity entity = editor.getFilmPanel().getController().getCurrentEntity();
        Form form = entity == null ? null : entity.getForm();
        if ((form == null || (form instanceof ModelForm mf && (mf.model.get() == null || mf.model.get().isBlank())))
            && editor.getReplay() != null)
        {
            form = editor.getReplay().form.get();
        }
        Form root = form == null ? null : FormUtils.getRoot(form);

        if (root instanceof ModelForm modelForm && modelForm.model.get() != null && !modelForm.model.get().isBlank())
        {
            return modelForm;
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public UIKeyframeSheet createBoneSheet(
        UIPovEditor editor,
        String sheetId,
        String bone,
        KeyframeChannel<PoseTransform> channel,
        int colorIndex)
    {
        ModelForm handForm = editor.getHandEditorForm();
        int color = UIKeyframeEditor.COLORS[colorIndex % UIKeyframeEditor.COLORS.length];
        PoseTransform defaultBoneTransform = handForm.pose.get() != null && handForm.pose.get().get(bone) != null
            ? copyPose(handForm.pose.get().get(bone))
            : new PoseTransform();

        ValueTransform property = new ValueTransform(sheetId, copyPose(defaultBoneTransform));
        UIKeyframeSheet sheet = new UIKeyframeSheet(
            sheetId,
            IKey.constant(bone),
            color,
            (KeyframeChannel) channel,
            property,
            true);

        sheet.icon(Icons.LIMB);
        sheet.form(handForm);
        sheet.seed(() -> copyPose(channel.interpolate(
            editor.getReplayTick(),
            defaultBoneTransform)));
        editor.addPendingSheet(sheet);

        return sheet;
    }

    /** Hand-space equivalent of BBS's FilmBoneWorldProvider. The captured full
     * bone matrix includes the first-person arm root and every parent bone, so
     * UITransform can correctly convert Global edits back into local pose data. */
    public boolean getWorldMatrix(UIPovEditor editor, Matrix4f output)
    {
        String selected = editor.getGizmoBone();
        Matrix4f matrix = selected == null ? null : PovHandMatrices.getFull(selected);

        if (matrix == null)
        {
            return false;
        }

        output.set(matrix);

        return true;
    }

    private boolean hasPoseKeys(UIPovEditor editor, RecordedHandData hand, String bone)
    {
        KeyframeChannel<?> track = boneTrack(editor, hand, bone);

        if (!hand.pose.isEmpty() || track != null && !track.isEmpty())
        {
            return true;
        }

        for (KeyframeChannel<?> channel : hand.poseTracks.tracks.values())
        {
            if (channel.getId().startsWith("pose_overlay") && !channel.isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    private KeyframeChannel<?> boneTrack(UIPovEditor editor, RecordedHandData hand, String bone)
    {
        HandBoneUtils.HandBones bones = HandBoneUtils.collect(ModelFormRenderer.getModel(editor.getHandEditorForm()));

        if (bone.equals(bones.mainRoot()))
        {
            return hand.rightPose;
        }

        if (bone.equals(bones.offRoot()))
        {
            return hand.leftPose;
        }

        return hand.poseTracks.get(TrackId.bone("", bone));
    }

    private static PoseTransform copyPose(PoseTransform pose)
    {
        PoseTransform copy = new PoseTransform();
        copy.copy(pose);
        return copy;
    }
}
