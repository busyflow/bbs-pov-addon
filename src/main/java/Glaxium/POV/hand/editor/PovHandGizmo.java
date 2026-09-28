package Glaxium.POV.hand.editor;

import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class PovHandGizmo {
   private PovHandGizmo() {
   }

   public static void captureVisual() {
      PovHandGizmo.Context context = getContext();
      if (context != null) {
         capture(context, null);
      }
   }

   public static void renderStencil(StencilMap stencilMap) {
      PovHandGizmo.Context context = getContext();
      if (context != null && stencilMap != null) {
         capture(context, stencilMap);
      }
   }

   public static UIPropTransform getTransform() {
      PovHandGizmo.Context context = getContext();
      return context == null ? null : context.transform;
   }

   private static PovHandGizmo.Context getContext() {
      UIFilmPanel panel = PovReplaySettings.getFilmPanel();
      if (panel != null
         && panel.getController().getPovMode() == 6
         && panel instanceof UIFilmPanelPovAccess access
         && access.bbsPov$getEditor() != null
         && access.bbsPov$getEditor().isPoseGizmoSection()) {
         UIPropTransform transform = access.bbsPov$getEditor().getHandGizmoTransform();
         String selected = access.bbsPov$getEditor().getGizmoBone();
         if (transform != null && selected != null && !selected.isBlank()) {
            TransformSpace space = access.bbsPov$getEditor().keyframeEditor.getBoneSpace();
            Matrix4f bone = PovHandMatrices.getForSpace(selected, space);
            return bone == null ? null : new PovHandGizmo.Context(panel.getController(), transform, bone, space);
         }

         return null;
      }

      return null;
   }

   private static void capture(PovHandGizmo.Context context, StencilMap stencilMap) {
      MatrixStack matrices = new MatrixStack();
      MatrixStackUtils.multiply(matrices, context.bone);
      Gizmo.INSTANCE.trackGesture(context.transform != null ? context.transform.getGesture() : null);
      Gizmo.INSTANCE.reorientForSpace(matrices, context.space, null, null);
      if (stencilMap == null) {
         Gizmo.INSTANCE.captureVisual(matrices);
      } else {
         Gizmo.INSTANCE.renderStencil(matrices);
      }
   }

   private static record Context(UIFilmController controller, UIPropTransform transform, Matrix4f bone, TransformSpace space) {
   }
}
