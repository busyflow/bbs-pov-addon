package Glaxium.POV.bodypart;

import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.Map;
import java.util.WeakHashMap;

/** BodyPart sync, hover-frame cache, and picker/render pass used by hand playback. */
public final class PovBodyPartPlayback
{
    private static final Map<ModelForm, BaseType> LAST_SYNCED_PARTS = new WeakHashMap<>();
    private static final Map<ModelForm, Map<String, BodyPartFrame>> BODY_PART_FRAMES = new WeakHashMap<>();

    private PovBodyPartPlayback()
    {
    }

    /** Mouse drags update keys between the visible pass and the picker pass.
     * Keep the picker on the values that produced the displayed pixels. Copies
     * are required: a single Pose key may be edited in place by the UI. */
    public static void syncHoverFrame(ModelForm root)
    {
        if (!PovHandPicking.isStencilPass())
        {
            Map<String, BodyPartFrame> frame = new java.util.HashMap<>();
            captureFrame(root, "", frame);
            BODY_PART_FRAMES.put(root, frame);
            return;
        }

        Map<String, BodyPartFrame> frame = BODY_PART_FRAMES.get(root);
        if (frame == null)
        {
            return;
        }

        for (Map.Entry<String, BodyPartFrame> entry : frame.entrySet())
        {
            Form target = FormUtils.getForm(root, entry.getKey());
            BodyPartFrame saved = entry.getValue();
            if (target != null)
            {
                target.transform.setRuntimeValue(saved.transform.copy());
                target.transformOverlay.setRuntimeValue(saved.transformOverlay.copy());
                for (int j = 0; j < Math.min(target.additionalTransforms.size(), saved.additionalTransforms.size()); j++)
                {
                    target.additionalTransforms.get(j).setRuntimeValue(saved.additionalTransforms.get(j).copy());
                }

                if (target instanceof ModelForm model)
                {
                    if (saved.pose != null)
                    {
                        model.pose.setRuntimeValue(saved.pose.copy());
                    }
                    if (saved.poseOverlay != null)
                    {
                        model.poseOverlay.setRuntimeValue(saved.poseOverlay.copy());
                    }
                    for (int j = 0; j < Math.min(model.additionalOverlays.size(), saved.additionalPoses.size()); j++)
                    {
                        model.additionalOverlays.get(j).setRuntimeValue(saved.additionalPoses.get(j).copy());
                    }
                }
            }
        }
    }

    private static void captureFrame(Form root, String prefix, Map<String, BodyPartFrame> frame)
    {
        int index = 0;
        for (var part : root.parts.getAllTyped())
        {
            Form child = part.getForm();
            String path = prefix.isEmpty() ? Integer.toString(index) : prefix + "/" + index;
            if (child != null)
            {
                java.util.List<Transform> addT = new java.util.ArrayList<>();
                for (var t : child.additionalTransforms)
                {
                    addT.add(t.get().copy());
                }

                Pose p = null;
                Pose pOverlay = null;
                java.util.List<Pose> addP = new java.util.ArrayList<>();
                if (child instanceof ModelForm model)
                {
                    p = model.pose.get().copy();
                    pOverlay = model.poseOverlay.get().copy();
                    for (var o : model.additionalOverlays)
                    {
                        addP.add(o.get().copy());
                    }
                }

                frame.put(path, new BodyPartFrame(
                    child.transform.get().copy(),
                    child.transformOverlay.get().copy(),
                    addT,
                    p,
                    pOverlay,
                    addP));
                captureFrame(child, path, frame);
            }
            index++;
        }
    }

    private record BodyPartFrame(
        Transform transform,
        Transform transformOverlay,
        java.util.List<Transform> additionalTransforms,
        Pose pose,
        Pose poseOverlay,
        java.util.List<Pose> additionalPoses) {}

    /** Keep the nested forms and their renderers alive between Film frames.
     * Re-deserializing the manager every render resets native action/procedural
     * animators before they can advance. Runtime keyframe values are not part
     * of toData(), so this only refreshes when the saved bodypart setup changes. */
    public static void sync(ModelForm target, ModelForm source)
    {
        BaseType sourceData = source.parts.toData();
        BaseType last = LAST_SYNCED_PARTS.get(target);

        if (last == null || !BaseType.equals(last, sourceData))
        {
            LAST_SYNCED_PARTS.put(target, sourceData);
            target.parts.fromData(sourceData);
        }
    }

    /**
     * [PORTING NOTE: BBS FILM STENCIL PICKING FIX]
     * Problem: When porting to new BBS versions, clicking on bodyparts in Film editor stops working
     * and the white hover outline does not appear.
     * Root Cause: PovHandPlayback.render() calls render(povForm, light) during BOTH normal frame rendering
     * and the picking pass. In earlier versions, this method had:
     *     if (!PovHandPicking.isStencilPass()) { render(povForm, light, null); }
     * which silently dropped the bodypart render when isStencilPass() was true! As a result, 0 pixels were drawn
     * into the stencil framebuffer for body parts, making them unpickable.
     * Fix: Always route isStencilPass() to render(povForm, light, PovHandPicking.getStencilMap()).
     */
    public static void render(ModelForm povForm, int light)
    {
        if (!PovHandPicking.isStencilPass())
        {
            render(povForm, light, null);
        }
        else
        {
            render(povForm, light, PovHandPicking.getStencilMap());
        }
    }

    public static void render(ModelForm povForm, int light, StencilMap stencilMap)
    {
        if (povForm == null || povForm.parts.getAllTyped().isEmpty())
        {
            return;
        }

        FormRenderer<?> formRenderer = FormUtilsClient.getRenderer(povForm);

        if (formRenderer instanceof PovBodyPartRenderer renderer)
        {
            renderer.bbsPov$renderBodyParts(light, stencilMap);
        }
    }
}
