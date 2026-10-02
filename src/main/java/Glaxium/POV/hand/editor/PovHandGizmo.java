package Glaxium.POV.hand.editor;

import Glaxium.POV.hand.render.PovHandMatrices;

import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Bridges a screen-space POV hand bone into BBS's native gizmo pipeline. */
public final class PovHandGizmo
{
    private PovHandGizmo() {}

    /** Must run inside the visible first-person hand pass. Gizmo.captureVisual()
     * captures both the model matrix and the currently active hand projection. */
    public static void captureVisual()
    {
        Context context = getContext();

        if (context != null)
        {
            capture(context, null);
        }
    }

    /** Runs in the matching hand stencil framebuffer so the native handles can
     * be clicked without changing the selected POV keyframe underneath them. */
    public static void renderStencil(StencilMap stencilMap)
    {
        Context context = getContext();

        if (context != null && stencilMap != null)
        {
            capture(context, stencilMap);
        }
    }

    public static UIPropTransform getTransform()
    {
        Context context = getContext();

        return context == null ? null : context.transform;
    }

    private static Context getContext()
    {
        UIFilmPanel panel = PovReplaySettings.getFilmPanel();

        if (panel == null || panel.getController().getPovMode() != PovCameraMode.POV
            || !(panel instanceof UIFilmPanelPovAccess access)
            || access.bbsPov$getEditor() == null
            || !access.bbsPov$getEditor().isPoseGizmoSection())
        {
            return null;
        }

        UIPropTransform transform = access.bbsPov$getEditor().getHandGizmoTransform();
        String selected = access.bbsPov$getEditor().getGizmoBone();

        if (transform == null || selected == null || selected.isBlank())
        {
            return null;
        }

        TransformSpace space = access.bbsPov$getEditor().keyframeEditor.getBoneSpace();
        Matrix4f bone = PovHandMatrices.getForSpace(selected, space);

        return bone == null
            ? null
            : new Context(panel.getController(), transform, bone, space);
    }

    private static void capture(Context context, StencilMap stencilMap)
    {
        MatrixStack matrices = new MatrixStack();
        MatrixStackUtils.multiply(matrices, context.bone);

        Gizmo.INSTANCE.trackGesture(context.transform != null ? context.transform.getGesture() : null);
        /* getForSpace() already supplies the exact first-person Local/Parent/
         * Global/View/World basis. Keep BBS's renderer from replacing it with
         * the Replay Editor's actor basis. */
        Gizmo.INSTANCE.reorientForSpace(matrices, context.space, null, null);

        if (stencilMap == null)
        {
            Gizmo.INSTANCE.captureVisual(matrices);
        }
        else
        {
            Gizmo.INSTANCE.renderStencil(matrices);
        }
    }

    private record Context(
        UIFilmController controller,
        UIPropTransform transform,
        Matrix4f bone,
        TransformSpace space)
    {}
}
