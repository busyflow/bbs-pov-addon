package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.bodypart.PovBodyPartGizmoDrag;
import Glaxium.POV.editor.UIPovHandEditor;
import Glaxium.POV.hand.render.PovHandMatrices;
import java.util.function.Supplier;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.forms.editors.UIBodyPartEditor;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.UIForms;
import mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIFormEditor.class},
   remap = false
)
public abstract class UIFormEditorPovMixin {
   @Shadow
   public UIForm editor;
   @Shadow
   public UIPickableFormRenderer renderer;
   @Shadow
   public UIBodyPartEditor bodyPartEditor;
   @Shadow
   public UIForms formsList;

   @Shadow
   public abstract boolean isBodyPartGizmoMode();

   @Shadow
   public abstract TransformSpace getGizmoSpace();

   @Inject(
      method = {"buildGizmoDrag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$buildPovHandGizmoDrag(UIPropTransform transformEditor, float transition, CallbackInfoReturnable<GizmoDrag> info) {
      if (UIPovHandEditor.isActive() && transformEditor != null && transformEditor.getTransform() != null) {
         UIPovHandEditor handEditor = UIPovHandEditor.getActive();
         if (handEditor != null) {
            if (this.isBodyPartGizmoMode() && this.formsList != null) {
               FormEntry entry = (FormEntry)this.formsList.getCurrentFirst();
               if (entry != null && entry.part != null) {
                  int partIndex = UIPovHandEditor.findBodyPartIndex(entry.part.getForm());
                  String formPath = FormUtils.getPath(entry.part.getForm());
                  String key = formPath != null && !formPath.isEmpty() ? formPath : String.valueOf(partIndex >= 0 ? partIndex : 0);
                  String fallbackKey1 = partIndex >= 0 ? String.valueOf(partIndex) : "";
                  String fallbackKey2 = entry.part.getId() != null ? entry.part.getId() : "";
                  Transform transform = transformEditor.getTransform();
                  Transform baseline = transform.copy();
                  Matrix4f captured = PovHandMatrices.getFull(key);
                  if (captured == null && !fallbackKey1.isEmpty()) {
                     captured = PovHandMatrices.getFull(fallbackKey1);
                  }

                  if (captured == null && !fallbackKey2.isEmpty()) {
                     captured = PovHandMatrices.getFull(fallbackKey2);
                  }

                  Matrix4f parentBase = UIPovHandEditor.getBodyPartParent(entry.part);
                  Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);
                  Matrix4f evaluated = PovHandMatrices.evaluateFull(key, baseline, transform);
                  if (evaluated == null && !fallbackKey1.isEmpty()) {
                     evaluated = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                  }

                  if (evaluated == null && !fallbackKey2.isEmpty()) {
                     evaluated = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                  }

                  if (evaluated == null) {
                     evaluated = base;
                  }

                  if (evaluated != null) {
                     captured = evaluated;
                  }

                  Camera camera = this.renderer.camera;
                  Area viewport = handEditor.getGapArea();
                  Vector3d dragOrigin = captured == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(captured.getTranslation(new Vector3f()));
                  GizmoDrag drag = new PovBodyPartGizmoDrag().setup(camera, viewport, dragOrigin);
                  bbsPov$configureBodyPartDrag(drag, dragOrigin);
                  Matrix4f fallbackMatrix = captured == null ? new Matrix4f().translation(0.0F, 0.0F, -1.0F) : new Matrix4f(captured);
                  Supplier<Matrix4f> matrix = () -> {
                     Matrix4f value = PovHandMatrices.evaluateFull(key, baseline, transform);
                     if (value == null && !fallbackKey1.isEmpty()) {
                        value = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                     }

                     if (value == null && !fallbackKey2.isEmpty()) {
                        value = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                     }

                     if (value == null) {
                        Matrix4f currentBase = new Matrix4f(parentBase);
                        if (transform != null) {
                           Matrix4f local = new Matrix4f();
                           transform.setupMatrix(local);
                           currentBase.mul(local);
                        }

                        value = currentBase;
                     }

                     return value == null ? new Matrix4f(fallbackMatrix) : value;
                  };
                  TransformSpace space = transformEditor.getSpace();
                  Matrix3f displayedBasis = PovHandMatrices.getBasisForSpace(key, space);
                  if (displayedBasis == null && !fallbackKey1.isEmpty()) {
                     displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey1, space);
                  }

                  if (displayedBasis == null && !fallbackKey2.isEmpty()) {
                     displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey2, space);
                  }

                  Matrix3f globalBasis = PovHandMatrices.getGlobalBasis(key);
                  if (globalBasis == null && !fallbackKey1.isEmpty()) {
                     globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey1);
                  }

                  if (globalBasis == null && !fallbackKey2.isEmpty()) {
                     globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey2);
                  }

                  if (displayedBasis != null) {
                     drag.gizmoWorldAxes.set(displayedBasis);
                  }

                  if (globalBasis != null) {
                     drag.setGlobalAxes(globalBasis);
                  }

                  drag.setRotateAxes(GizmoDrag.computeRotateAxes(transform, matrix));
                  drag.setJacobian(GizmoDrag.computeTranslateJacobian(transform, () -> matrix.get().getTranslation(new Vector3f())));
                  Matrix4f localMat = PovHandMatrices.getForSpace(key, TransformSpace.LOCAL);
                  if (localMat == null && !fallbackKey1.isEmpty()) {
                     localMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.LOCAL);
                  }

                  if (localMat == null && !fallbackKey2.isEmpty()) {
                     localMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.LOCAL);
                  }

                  if (localMat == null) {
                     localMat = base;
                  }

                  Matrix4f parentMat = PovHandMatrices.getForSpace(key, TransformSpace.PARENT);
                  if (parentMat == null && !fallbackKey1.isEmpty()) {
                     parentMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.PARENT);
                  }

                  if (parentMat == null && !fallbackKey2.isEmpty()) {
                     parentMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.PARENT);
                  }

                  if (parentMat == null) {
                     parentMat = parentBase;
                  }

                  drag.setFrameAxes(localMat, parentMat);
                  info.setReturnValue(drag);
                  return;
               }
            }

            if (this.editor instanceof UIModelForm modelForm && modelForm.modelPanel != null && modelForm.modelPanel.poseEditor != null) {
               String bone = (String)modelForm.modelPanel.poseEditor.groups.list.getCurrentFirst();
               if (bone == null || bone.isEmpty()) {
                  return;
               }

               if (modelForm.form == handEditor.getRootForm()) {
                  Matrix4f capturedx = PovHandMatrices.getFull(bone);
                  Transform transformx = transformEditor.getTransform();
                  Transform baselinex = transformx.copy();
                  Matrix4f evaluatedx = PovHandMatrices.evaluateFull(bone, baselinex, transformx);
                  if (evaluatedx != null) {
                     capturedx = evaluatedx;
                  }

                  Camera camerax = this.renderer.camera;
                  Area viewportx = handEditor.getGapArea();
                  Vector3d dragOriginx = capturedx == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(capturedx.getTranslation(new Vector3f()));
                  GizmoDrag dragx = new PovBodyPartGizmoDrag().setup(camerax, viewportx, dragOriginx);
                  bbsPov$configureBodyPartDrag(dragx, dragOriginx);
                  Matrix4f fallbackMatrixx = capturedx == null ? new Matrix4f().translation(0.0F, 0.0F, -1.0F) : new Matrix4f(capturedx);
                  Supplier<Matrix4f> matrixx = () -> {
                     Matrix4f value = PovHandMatrices.evaluateFull(bone, baseline, transform);
                     return value == null ? new Matrix4f(fallbackMatrix) : value;
                  };
                  TransformSpace spacex = modelForm.getGizmoSpace();
                  Matrix3f displayedBasisx = PovHandMatrices.getBasisForSpace(bone, spacex);
                  Matrix3f globalBasisx = PovHandMatrices.getGlobalBasis(bone);
                  if (displayedBasisx != null) {
                     dragx.gizmoWorldAxes.set(displayedBasisx);
                  }

                  if (globalBasisx != null) {
                     dragx.setGlobalAxes(globalBasisx);
                  }

                  dragx.setRotateAxes(GizmoDrag.computeRotateAxes(transformx, matrixx));
                  dragx.setJacobian(GizmoDrag.computeTranslateJacobian(transformx, () -> matrix.get().getTranslation(new Vector3f())));
                  dragx.setFrameAxes(PovHandMatrices.getForSpace(bone, TransformSpace.LOCAL), PovHandMatrices.getForSpace(bone, TransformSpace.PARENT));
                  info.setReturnValue(dragx);
               } else if (modelForm.form != null) {
                  ModelForm bodyPartModelForm = (ModelForm)modelForm.form;
                  BodyPart part = UIPovHandEditor.findBodyPart(modelForm.form);
                  if (part == null) {
                     return;
                  }

                  int partIndexx = UIPovHandEditor.findBodyPartIndex(modelForm.form);
                  String formPathx = FormUtils.getPath(modelForm.form);
                  String keyx = (
                        formPathx != null && !formPathx.isEmpty()
                           ? formPathx
                           : String.valueOf(partIndexx >= 0 ? partIndexx : (part.getId() != null ? part.getId() : ""))
                     )
                     + "/"
                     + bone;
                  String fallbackKey1x = (partIndexx >= 0 ? String.valueOf(partIndexx) : "") + "/" + bone;
                  String fallbackKey2x = (part.getId() != null ? part.getId() : "") + "/" + bone;
                  Matrix4f capturedxx = PovHandMatrices.getFull(keyx);
                  if (capturedxx == null && !fallbackKey1x.equals(keyx)) {
                     capturedxx = PovHandMatrices.getFull(fallbackKey1x);
                  }

                  if (capturedxx == null && !fallbackKey2x.equals(keyx)) {
                     capturedxx = PovHandMatrices.getFull(fallbackKey2x);
                  }

                  Transform transformxx = transformEditor.getTransform();
                  Transform baselinexx = transformxx.copy();
                  Matrix4f bodyPartBase = UIPovHandEditor.getBodyPartBase(part);
                  Matrix4f evaluatedxx = PovHandMatrices.evaluateFull(keyx, baselinexx, transformxx);
                  if (evaluatedxx == null && !fallbackKey1x.equals(keyx)) {
                     evaluatedxx = PovHandMatrices.evaluateFull(fallbackKey1x, baselinexx, transformxx);
                  }

                  if (evaluatedxx == null && !fallbackKey2x.equals(keyx)) {
                     evaluatedxx = PovHandMatrices.evaluateFull(fallbackKey2x, baselinexx, transformxx);
                  }

                  if (evaluatedxx == null) {
                     evaluatedxx = UIPovHandEditor.evaluateBodyPartBoneMatrix(bodyPartModelForm, bone, baselinexx, transformxx, bodyPartBase);
                  }

                  if (evaluatedxx != null) {
                     capturedxx = evaluatedxx;
                  }

                  Camera cameraxx = this.renderer.camera;
                  Area viewportxx = handEditor.getGapArea();
                  Vector3d dragOriginxx = capturedxx == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(capturedxx.getTranslation(new Vector3f()));
                  GizmoDrag dragxx = new PovBodyPartGizmoDrag().setup(cameraxx, viewportxx, dragOriginxx);
                  bbsPov$configureBodyPartDrag(dragxx, dragOriginxx);
                  Matrix4f fallbackMatrixxx = capturedxx == null ? new Matrix4f().translation(0.0F, 0.0F, -1.0F) : new Matrix4f(capturedxx);
                  Supplier<Matrix4f> matrixxx = () -> {
                     Matrix4f value = PovHandMatrices.evaluateFull(key, baseline, transform);
                     if (value == null && !fallbackKey1.equals(key)) {
                        value = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                     }

                     if (value == null && !fallbackKey2.equals(key)) {
                        value = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                     }

                     if (value == null) {
                        value = UIPovHandEditor.evaluateBodyPartBoneMatrix(bodyPartModelForm, bone, baseline, transform, bodyPartBase);
                     }

                     return value == null ? new Matrix4f(fallbackMatrix) : value;
                  };
                  TransformSpace spacexx = modelForm.getGizmoSpace();
                  Matrix3f displayedBasisxx = PovHandMatrices.getBasisForSpace(keyx, spacexx);
                  if (displayedBasisxx == null && !fallbackKey1x.equals(keyx)) {
                     displayedBasisxx = PovHandMatrices.getBasisForSpace(fallbackKey1x, spacexx);
                  }

                  if (displayedBasisxx == null && !fallbackKey2x.equals(keyx)) {
                     displayedBasisxx = PovHandMatrices.getBasisForSpace(fallbackKey2x, spacexx);
                  }

                  Matrix3f globalBasisxx = PovHandMatrices.getGlobalBasis(keyx);
                  if (globalBasisxx == null && !fallbackKey1x.equals(keyx)) {
                     globalBasisxx = PovHandMatrices.getGlobalBasis(fallbackKey1x);
                  }

                  if (globalBasisxx == null && !fallbackKey2x.equals(keyx)) {
                     globalBasisxx = PovHandMatrices.getGlobalBasis(fallbackKey2x);
                  }

                  if (displayedBasisxx != null) {
                     dragxx.gizmoWorldAxes.set(displayedBasisxx);
                  }

                  if (globalBasisxx != null) {
                     dragxx.setGlobalAxes(globalBasisxx);
                  }

                  dragxx.setRotateAxes(GizmoDrag.computeRotateAxes(transformxx, matrixxx));
                  dragxx.setJacobian(GizmoDrag.computeTranslateJacobian(transformxx, () -> matrix.get().getTranslation(new Vector3f())));
                  Matrix4f localMatx = PovHandMatrices.getForSpace(keyx, TransformSpace.LOCAL);
                  if (localMatx == null && !fallbackKey1x.equals(keyx)) {
                     localMatx = PovHandMatrices.getForSpace(fallbackKey1x, TransformSpace.LOCAL);
                  }

                  if (localMatx == null && !fallbackKey2x.equals(keyx)) {
                     localMatx = PovHandMatrices.getForSpace(fallbackKey2x, TransformSpace.LOCAL);
                  }

                  if (localMatx == null) {
                     localMatx = UIPovHandEditor.getBodyPartBoneMatrix(bodyPartModelForm, bone, true, bodyPartBase);
                  }

                  Matrix4f parentMatx = PovHandMatrices.getForSpace(keyx, TransformSpace.PARENT);
                  if (parentMatx == null && !fallbackKey1x.equals(keyx)) {
                     parentMatx = PovHandMatrices.getForSpace(fallbackKey1x, TransformSpace.PARENT);
                  }

                  if (parentMatx == null && !fallbackKey2x.equals(keyx)) {
                     parentMatx = PovHandMatrices.getForSpace(fallbackKey2x, TransformSpace.PARENT);
                  }

                  if (parentMatx == null) {
                     parentMatx = UIPovHandEditor.getBodyPartBoneMatrix(bodyPartModelForm, bone, false, bodyPartBase);
                  }

                  dragxx.setFrameAxes(localMatx, parentMatx);
                  info.setReturnValue(dragxx);
               }
            }
         }
      }
   }

   @Unique
   private static void bbsPov$configureBodyPartDrag(GizmoDrag drag, Vector3d origin) {
      drag.view.identity();
      drag.cameraOrigin.set(0.0, 0.0, 0.0);
      drag.gizmoOrigin.set(origin);
   }

   @Inject(
      method = {"getOrigin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getBodyPartGizmoOrigin(float transition, CallbackInfoReturnable<Matrix4f> info) {
      if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null) {
         FormEntry entry = (FormEntry)this.formsList.getCurrentFirst();
         if (entry != null && entry.part != null) {
            Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);
            info.setReturnValue(new Matrix4f(base));
         }
      }
   }

   @Inject(
      method = {"getOriginMatrix"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getBodyPartGizmoOriginMatrix(float transition, CallbackInfoReturnable<Matrix4f> info) {
      if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null) {
         FormEntry entry = (FormEntry)this.formsList.getCurrentFirst();
         if (entry != null && entry.part != null) {
            Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);
            info.setReturnValue(new Matrix4f(base));
         }
      }
   }

   @Inject(
      method = {"getParentOriginMatrix"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getBodyPartGizmoParentOriginMatrix(float transition, CallbackInfoReturnable<Matrix4f> info) {
      if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null) {
         FormEntry entry = (FormEntry)this.formsList.getCurrentFirst();
         if (entry != null && entry.part != null) {
            Matrix4f parent = UIPovHandEditor.getBodyPartParent(entry.part);
            info.setReturnValue(new Matrix4f(parent));
         }
      }
   }
}
