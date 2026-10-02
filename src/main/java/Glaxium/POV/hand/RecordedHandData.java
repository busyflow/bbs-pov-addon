package Glaxium.POV.hand;

import Glaxium.POV.hand.editor.HandBoneUtils;

import Glaxium.POV.hand.recording.HandRecorder;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.ui.ValueStringKeys;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;

/** Serialized hand-only channels owned by a replay's keyframe group. */
public final class RecordedHandData
{
    public static final String DEFAULT_MODEL = "player/steve";
    public static final Link DEFAULT_TEXTURE = Link.assets("models/player/steve/steve.png");

    public final ValueForm baseForm = new ValueForm("pov_base_form");
    public final KeyframeChannel<Boolean> visible = channel("visible", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<String> model = channel("model", KeyframeFactories.STRING);
    public final KeyframeChannel<Link> texture = channel("texture", KeyframeFactories.LINK);
    public final KeyframeChannel<Color> color = channel("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Color> colorOverlay = channel("color_overlay", KeyframeFactories.COLOR);
    public final KeyframeChannel<Transform> cameraOffset = channel("camera_offset", KeyframeFactories.TRANSFORM);
    /** Native BBS pose editor: bone picker, Fix, color, lighting and full transform. */
    public final KeyframeChannel<Pose> pose = channel("pose", KeyframeFactories.POSE);
    /** Two virtual bones which transform the rendered main-hand and off-hand items. */
    public final KeyframeChannel<Pose> itemPose = channel("item_pose", KeyframeFactories.POSE);
    /** Physical model-arm visibility. Items remain visible when their arm is hidden. */
    public final KeyframeChannel<Boolean> rightHandVisible = channel("right_hand_visible", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> leftHandVisible = channel("left_hand_visible", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<PoseTransform> rightPose = channel("right_pose", KeyframeFactories.POSE_TRANSFORM);
    public final KeyframeChannel<PoseTransform> leftPose = channel("left_pose", KeyframeFactories.POSE_TRANSFORM);
    /** Dynamic per-limb tracks for every descendant of either configured FP hand root. */
    public final FormProperties poseTracks = new FormProperties("pov_hand_pose_tracks");
    /** Native BBS form-property animation for forms attached as POV bodyparts. */
    public final FormProperties bodyPartTracks = new FormProperties("pov_bodypart_tracks");
    public final KeyframeChannel<Float> rightSwingProgress = channel("right_swing_progress", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> leftSwingProgress = channel("left_swing_progress", KeyframeFactories.FLOAT);
    /** Vanilla equip lowering: 0 is fully equipped and 1 is fully lowered. */
    public final KeyframeChannel<Float> mainEquipProgress = channel("main_equip", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> offEquipProgress = channel("off_equip", KeyframeFactories.FLOAT);
    /** 0 = not using, 1 = main hand, 2 = off hand. */
    public final KeyframeChannel<Integer> activeHand = channel("active_hand", KeyframeFactories.INTEGER);
    public final KeyframeChannel<ItemStack> activeItem = channel("active_item", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<Boolean> showUseParticles = channel("show_use_particles", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Integer> useTime = channel("use_time", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Float> bobPhase = channel("bob_phase", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> bobStrength = channel("bob_strength", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> renderYaw = channel("render_yaw", KeyframeFactories.FLOAT);
    public final KeyframeChannel<Float> renderPitch = channel("render_pitch", KeyframeFactories.FLOAT);
    /** False = right-handed, true = left-handed. Boolean factory gives the editor a toggle. */
    public final KeyframeChannel<Boolean> mainArm = channel("main_arm", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> worldInteraction = channel("world_interaction", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Boolean> replayInteraction = channel("replay_interaction", KeyframeFactories.BOOLEAN);
    public final ValueStringKeys disabledTracks = new ValueStringKeys("pov_disabled_tracks");

    public final HandRecorder recording = new HandRecorder();

    private static <T> KeyframeChannel<T> channel(String id, IKeyframeFactory<T> factory)
    {
        return new KeyframeChannel<>("pov_hand_" + id, factory);
    }

    @SuppressWarnings("unchecked")
    public void addTo(ValueGroup group)
    {
        group.add(this.baseForm);
        group.add(this.disabledTracks);

        for (KeyframeChannel<?> channel : this.getOwnedChannels())
        {
            group.add(channel);
        }

        group.add(this.poseTracks);
        group.add(this.bodyPartTracks);
    }

    public void initializeReplayPreset()
    {
        this.insertPreset();
    }

    private void insertPreset()
    {
        this.recording.reset();
        /* Editor-authored tracks intentionally have no implicit keyframes and recording
         * never clears them: visible/model/texture/camera offset/pose/per-limb,
         * arm visibility and main arm. */
        reset(this.rightSwingProgress, 0F);
        reset(this.leftSwingProgress, 0F);
        reset(this.mainEquipProgress, 0F);
        reset(this.offEquipProgress, 0F);
        reset(this.activeHand, 0);
        reset(this.activeItem, ItemStack.EMPTY);
        reset(this.showUseParticles, true);
        reset(this.useTime, 0);
        reset(this.bobPhase, 0F);
        reset(this.bobStrength, 0F);
    }

    public boolean hasRecordedData()
    {
        return this.recording.hasRecorded();
    }

    public void addRecordingEndKeyframes(ReplayKeyframes replay, int endTick)
    {
        int tick = Math.max(0, endTick);

        for (KeyframeChannel<?> channel : this.getRecordedChannels())
        {
            addEndKeyframeUntyped(channel, tick);
        }
    }

    public KeyframeChannel<PoseTransform> getOrCreatePoseTrack(
        String bone,
        HandBoneUtils.HandBones bones)
    {
        if (bone.equals(bones.mainRoot()))
        {
            return this.rightPose;
        }

        if (bone.equals(bones.offRoot()))
        {
            return this.leftPose;
        }

        return (KeyframeChannel<PoseTransform>) (Object) this.poseTracks.register(
            TrackId.bone("", bone),
            KeyframeFactories.POSE_TRANSFORM);
    }

    public KeyframeChannel<Pose> getOrCreatePoseOverlay(ModelForm handForm, String key)
    {
        if (handForm == null || key == null || key.isBlank())
        {
            return null;
        }

        return this.poseTracks.getOrCreate(handForm, key);
    }

    public void applyPoseTracks(ModelForm form, float tick)
    {
        form.pose.setRuntimeValue(null);
        form.poseOverlay.setRuntimeValue(null);
        for (ValuePose overlay : form.additionalOverlays)
        {
            overlay.setRuntimeValue(null);
        }
        this.poseTracks.applyProperties(form, tick);
    }

    public KeyframeChannel<?> getOrCreateBodyPartTrack(ModelForm root, String partId, String property)
    {
        if (root == null || partId == null || partId.isBlank() || property == null || property.isBlank())
        {
            return null;
        }

        return this.bodyPartTracks.getOrCreate(root, TrackId.property(partId, property));
    }

    public KeyframeChannel<?> getOrCreateBodyPartTrack(ModelForm root, int partIndex, String property)
    {
        if (root == null || partIndex < 0 || root.parts.getAllTyped().size() <= partIndex)
        {
            return null;
        }

        mchorse.bbs_mod.forms.forms.BodyPart part = root.parts.getAllTyped().get(partIndex);
        return getOrCreateBodyPartTrack(root, part.getId(), property);
    }

    /** Apply after the static Base POV bodyparts have been copied onto the
     * frame-local hand form, so empty tracks naturally retain their setup values. */
    public void applyBodyPartTracks(ModelForm form, float tick)
    {
        this.bodyPartTracks.applyProperties(form, tick);
    }

    public KeyframeChannel<?>[] getOwnedChannels()
    {
        return new KeyframeChannel<?>[] {
            this.visible, this.model, this.texture, this.color, this.colorOverlay, this.cameraOffset, this.pose, this.itemPose,
            this.rightHandVisible, this.leftHandVisible,
            this.rightPose, this.leftPose, this.rightSwingProgress, this.leftSwingProgress,
            this.mainEquipProgress, this.offEquipProgress, this.activeHand, this.activeItem, this.showUseParticles, this.useTime,
            this.bobPhase, this.bobStrength, this.renderYaw, this.renderPitch, this.mainArm,
            this.worldInteraction, this.replayInteraction
        };
    }

    private KeyframeChannel<?>[] getRecordedChannels()
    {
        return new KeyframeChannel<?>[] {
            this.rightSwingProgress, this.leftSwingProgress, this.mainEquipProgress,
            this.offEquipProgress, this.activeHand, this.activeItem, this.showUseParticles, this.useTime,
            this.bobPhase, this.bobStrength, this.renderYaw, this.renderPitch
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addEndKeyframeUntyped(KeyframeChannel<?> channel, int tick)
    {
        KeyframeChannel raw = channel;
        Object value = raw.interpolate(tick, null);

        /* An optional/manual channel can legitimately have no recording keys. Never
         * create a null keyframe: typed BBS factories assume values are non-null. */
        if (value == null)
        {
            return;
        }

        if (value instanceof Pose pose)
        {
            value = pose.copy();
        }
        else if (value instanceof Transform transform)
        {
            value = transform.copy();
        }
        else if (value instanceof Color color)
        {
            value = color.copy();
        }

        raw.insert(tick, value);
    }

    private static <T> void reset(KeyframeChannel<T> channel, T value)
    {
        channel.insert(0F, value);
    }

    public Form ensureBaseForm(Form replayForm)
    {
        Form value = this.baseForm.get();

        if (value != null)
        {
            ensureOverlays(value);
        }

        return value != null ? value : replayForm;
    }

    public static void ensureOverlays(Form form)
    {
        if (form == null)
        {
            return;
        }

        int additionalTransforms = BBSSettings.recordingTransformOverlays.get();

        while (form.additionalTransforms.size() < additionalTransforms)
        {
            int idx = form.additionalTransforms.size();
            ValueTransform vt = new ValueTransform("transform_overlay" + idx, new Transform());
            form.additionalTransforms.add(vt);
            form.add(vt);
        }

        if (form instanceof ModelForm modelForm)
        {
            modelForm.boneTracks.set(true);
            int additionalPoses = BBSSettings.recordingPoseOverlays.get();
            while (modelForm.additionalOverlays.size() < additionalPoses)
            {
                int idx = modelForm.additionalOverlays.size();
                ValuePose vp = new ValuePose("pose_overlay" + idx, new Pose());
                modelForm.additionalOverlays.add(vp);
                modelForm.add(vp);
            }
        }

        for (var part : form.parts.getAllTyped())
        {
            if (part.getForm() != null)
            {
                ensureOverlays(part.getForm());
            }
        }
    }

    public Pose getBasePose()
    {
        Form value = this.baseForm.get();
        if (value != null)
        {
            Form root = FormUtils.getRoot(value);
            if (root instanceof ModelForm modelForm && modelForm.pose.get() != null)
            {
                return modelForm.pose.get();
            }
        }
        return null;
    }
}
