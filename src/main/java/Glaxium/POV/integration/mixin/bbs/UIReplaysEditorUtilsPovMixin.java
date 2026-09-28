package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartGizmoDrag;
import Glaxium.POV.editor.UIPovEditor;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import java.util.function.Supplier;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIReplaysEditorUtils.class},
   remap = false
)
public class UIReplaysEditorUtilsPovMixin {
   @Inject(
      method = {"buildFilmGizmoDrag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$buildHandGizmoDrag(
      UIFilmPanel panel, Camera camera, Area viewport, UIPropTransform editor, float transition, CallbackInfoReturnable<GizmoDrag> info
   ) {
      if (panel instanceof UIFilmPanelPovAccess access) {
         UIPovEditor pov = access.bbsPov$getEditor();
         if (pov != null && pov.isPoseGizmoSection() && editor != null && editor.getTransform() != null) {
            UIKeyframeEditor keyframeEditor = pov.keyframeEditor;
            String bone = keyframeEditor == null ? null : pov.getGizmoBone();
            Matrix4f captured = bone == null ? null : PovHandMatrices.getFull(bone);
            Transform transform = editor.getTransform();
            Transform baseline = transform.copy();
            Matrix4f evaluated = bone == null ? null : PovHandMatrices.evaluateFull(bone, baseline, transform);
            if (evaluated != null) {
               captured = evaluated;
            } else if (bone == null) {
               Matrix4f local = new Matrix4f();
               local.translate(0.0F, 0.0F, -1.0F);
               transform.setupMatrix(local);
               captured = local;
            }

            Matrix4f handProjection = PovHandPicking.getProjection();
            Vector3d dragOrigin = captured == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(captured.getTranslation(new Vector3f()));
            GizmoDrag drag = new PovBodyPartGizmoDrag().setup(camera, viewport, dragOrigin);
            if (handProjection != null) {
               drag.projection.set(handProjection);
            }

            drag.view.identity();
            drag.cameraOrigin.set(0.0, 0.0, 0.0);
            drag.gizmoOrigin.set(dragOrigin);
            Supplier<Matrix4f> matrix = () -> {
               if (bone != null) {
                  Matrix4f value = PovHandMatrices.evaluateFull(bone, baseline, transform);
                  return value == null ? new Matrix4f().translation(0.0F, 0.0F, -1.0F) : value;
               } else {
                  Matrix4f local = new Matrix4f();
                  local.translate(0.0F, 0.0F, -1.0F);
                  transform.setupMatrix(local);
                  return local;
               }
            };
            TransformSpace space = keyframeEditor != null ? keyframeEditor.getBoneSpace() : TransformSpace.LOCAL;
            Matrix3f displayedBasis = null;
            Matrix3f globalBasis = null;
            if (bone != null) {
               displayedBasis = PovHandMatrices.getBasisForSpace(bone, space);
               globalBasis = PovHandMatrices.getGlobalBasis(bone);
            } else {
               displayedBasis = new Matrix3f();
               if (space == TransformSpace.LOCAL) {
                  matrix.get().get3x3(displayedBasis);
               } else {
                  displayedBasis.identity();
               }

               globalBasis = new Matrix3f().identity();
            }

            if (displayedBasis != null) {
               drag.gizmoWorldAxes.set(displayedBasis);
            }

            if (globalBasis != null) {
               drag.setGlobalAxes(globalBasis);
            }

            drag.setRotateAxes(GizmoDrag.computeRotateAxes(transform, matrix));
            drag.setJacobian(GizmoDrag.computeTranslateJacobian(transform, () -> matrix.get().getTranslation(new Vector3f())));
            Vector3f additiveBase = bone != null ? PovHandMatrices.getAdditiveRotationBase(bone, baseline) : null;
            drag.setAdditiveRotationBase(additiveBase);
            if (bone != null) {
               drag.setFrameAxes(PovHandMatrices.getForSpace(bone, TransformSpace.LOCAL), PovHandMatrices.getForSpace(bone, TransformSpace.PARENT));
            } else {
               drag.setFrameAxes(matrix.get(), new Matrix4f().identity().translation(0.0F, 0.0F, -1.0F));
            }

            info.setReturnValue(drag);
         }
      }
   }
}
