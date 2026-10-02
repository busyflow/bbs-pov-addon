package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.bodypart.PovBodyPartGizmoDrag;
import Glaxium.POV.editor.UIPovHandEditor;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
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

import java.util.function.Supplier;

@Mixin(value = UIFormEditor.class, remap = false)
public abstract class UIFormEditorPovMixin
{
    @Shadow public UIForm editor;
    @Shadow public UIPickableFormRenderer renderer;
    @Shadow public mchorse.bbs_mod.ui.forms.editors.UIBodyPartEditor bodyPartEditor;
    @Shadow public mchorse.bbs_mod.ui.forms.editors.UIForms formsList;
    @Shadow public abstract boolean isBodyPartGizmoMode();

    @Shadow public abstract TransformSpace getGizmoSpace();

    @Inject(method = "buildGizmoDrag", at = @At("HEAD"), cancellable = true)
    private void bbsPov$buildPovHandGizmoDrag(UIPropTransform transformEditor, float transition, CallbackInfoReturnable<GizmoDrag> info)
    {
        if (!UIPovHandEditor.isActive() || transformEditor == null || transformEditor.getTransform() == null)
        {
            return;
        }

        UIPovHandEditor handEditor = UIPovHandEditor.getActive();
        if (handEditor == null)
        {
            return;
        }

        if (this.isBodyPartGizmoMode() && this.formsList != null)
        {
            mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry entry = this.formsList.getCurrentFirst();
            if (entry != null && entry.part != null)
            {
                int partIndex = UIPovHandEditor.findBodyPartIndex(entry.part.getForm());
                String formPath = mchorse.bbs_mod.forms.FormUtils.getPath(entry.part.getForm());
                String key = formPath != null && !formPath.isEmpty() ? formPath : String.valueOf(partIndex >= 0 ? partIndex : 0);
                String fallbackKey1 = partIndex >= 0 ? String.valueOf(partIndex) : "";
                String fallbackKey2 = entry.part.getId() != null ? entry.part.getId() : "";

                Transform transform = transformEditor.getTransform();
                Transform baseline = transform.copy();

                Matrix4f captured = PovHandMatrices.getFull(key);
                if (captured == null && !fallbackKey1.isEmpty()) captured = PovHandMatrices.getFull(fallbackKey1);
                if (captured == null && !fallbackKey2.isEmpty()) captured = PovHandMatrices.getFull(fallbackKey2);

                Matrix4f parentBase = UIPovHandEditor.getBodyPartParent(entry.part);
                Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);

                Matrix4f evaluated = PovHandMatrices.evaluateFull(key, baseline, transform);
                if (evaluated == null && !fallbackKey1.isEmpty()) evaluated = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                if (evaluated == null && !fallbackKey2.isEmpty()) evaluated = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                if (evaluated == null)
                {
                    evaluated = base;
                }

                if (evaluated != null)
                {
                    captured = evaluated;
                }

                Camera camera = this.renderer.camera;
                Area viewport = handEditor.getGapArea();
                Vector3d dragOrigin = captured == null
                    ? new Vector3d(0D, 0D, -1D)
                    : new Vector3d(captured.getTranslation(new Vector3f()));

                GizmoDrag drag = new PovBodyPartGizmoDrag().setup(camera, viewport, dragOrigin);
                bbsPov$configureBodyPartDrag(drag, dragOrigin);

                Matrix4f fallbackMatrix = captured == null
                    ? new Matrix4f().translation(0F, 0F, -1F)
                    : new Matrix4f(captured);

                Supplier<Matrix4f> matrix = () ->
                {
                    Matrix4f value = PovHandMatrices.evaluateFull(key, baseline, transform);
                    if (value == null && !fallbackKey1.isEmpty()) value = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                    if (value == null && !fallbackKey2.isEmpty()) value = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                    if (value == null)
                    {
                        Matrix4f currentBase = new Matrix4f(parentBase);
                        if (transform != null)
                        {
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
                if (displayedBasis == null && !fallbackKey1.isEmpty()) displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey1, space);
                if (displayedBasis == null && !fallbackKey2.isEmpty()) displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey2, space);

                Matrix3f globalBasis = PovHandMatrices.getGlobalBasis(key);
                if (globalBasis == null && !fallbackKey1.isEmpty()) globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey1);
                if (globalBasis == null && !fallbackKey2.isEmpty()) globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey2);

                if (displayedBasis != null)
                {
                    drag.gizmoWorldAxes.set(displayedBasis);
                }
                if (globalBasis != null)
                {
                    drag.setGlobalAxes(globalBasis);
                }

                drag.setRotateAxes(GizmoDrag.computeRotateAxes(transform, matrix));
                drag.setJacobian(GizmoDrag.computeTranslateJacobian(
                    transform,
                    () -> matrix.get().getTranslation(new Vector3f())));

                Matrix4f localMat = PovHandMatrices.getForSpace(key, TransformSpace.LOCAL);
                if (localMat == null && !fallbackKey1.isEmpty()) localMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.LOCAL);
                if (localMat == null && !fallbackKey2.isEmpty()) localMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.LOCAL);
                if (localMat == null) localMat = base;

                Matrix4f parentMat = PovHandMatrices.getForSpace(key, TransformSpace.PARENT);
                if (parentMat == null && !fallbackKey1.isEmpty()) parentMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.PARENT);
                if (parentMat == null && !fallbackKey2.isEmpty()) parentMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.PARENT);
                if (parentMat == null) parentMat = parentBase;

                drag.setFrameAxes(localMat, parentMat);

                info.setReturnValue(drag);
                return;
            }
        }

        if (this.editor instanceof UIModelForm modelForm && modelForm.modelPanel != null && modelForm.modelPanel.poseEditor != null)
        {
            String bone = modelForm.modelPanel.poseEditor.groups.list.getCurrentFirst();
            if (bone == null || bone.isEmpty())
            {
                return;
            }

            if (modelForm.form == handEditor.getRootForm())
            {
                Matrix4f captured = PovHandMatrices.getFull(bone);
                Transform transform = transformEditor.getTransform();
                Transform baseline = transform.copy();
                Matrix4f evaluated = PovHandMatrices.evaluateFull(bone, baseline, transform);

                if (evaluated != null)
                {
                    captured = evaluated;
                }

                Camera camera = this.renderer.camera;
                Area viewport = handEditor.getGapArea();

                Vector3d dragOrigin = captured == null
                    ? new Vector3d(0D, 0D, -1D)
                    : new Vector3d(captured.getTranslation(new Vector3f()));

                GizmoDrag drag = new PovBodyPartGizmoDrag().setup(camera, viewport, dragOrigin);
                bbsPov$configureBodyPartDrag(drag, dragOrigin);

                Matrix4f fallbackMatrix = captured == null
                    ? new Matrix4f().translation(0F, 0F, -1F)
                    : new Matrix4f(captured);

                Supplier<Matrix4f> matrix = () ->
                {
                    Matrix4f value = PovHandMatrices.evaluateFull(bone, baseline, transform);
                    return value == null ? new Matrix4f(fallbackMatrix) : value;
                };

                TransformSpace space = modelForm.getGizmoSpace();
                Matrix3f displayedBasis = PovHandMatrices.getBasisForSpace(bone, space);
                Matrix3f globalBasis = PovHandMatrices.getGlobalBasis(bone);

                if (displayedBasis != null)
                {
                    drag.gizmoWorldAxes.set(displayedBasis);
                }
                if (globalBasis != null)
                {
                    drag.setGlobalAxes(globalBasis);
                }

                drag.setRotateAxes(GizmoDrag.computeRotateAxes(transform, matrix));
                drag.setJacobian(GizmoDrag.computeTranslateJacobian(
                    transform,
                    () -> matrix.get().getTranslation(new Vector3f())));

                drag.setFrameAxes(
                    PovHandMatrices.getForSpace(bone, TransformSpace.LOCAL),
                    PovHandMatrices.getForSpace(bone, TransformSpace.PARENT)
                );

                info.setReturnValue(drag);
            }
            else if (modelForm.form != null)
            {
                mchorse.bbs_mod.forms.forms.ModelForm bodyPartModelForm = modelForm.form;
                mchorse.bbs_mod.forms.forms.BodyPart part = UIPovHandEditor.findBodyPart(modelForm.form);
                if (part == null)
                {
                    return;
                }

                int partIndex = UIPovHandEditor.findBodyPartIndex(modelForm.form);
                String formPath = mchorse.bbs_mod.forms.FormUtils.getPath(modelForm.form);
                String key = (formPath != null && !formPath.isEmpty() ? formPath : String.valueOf(partIndex >= 0 ? partIndex : (part.getId() != null ? part.getId() : ""))) + "/" + bone;
                String fallbackKey1 = (partIndex >= 0 ? String.valueOf(partIndex) : "") + "/" + bone;
                String fallbackKey2 = (part.getId() != null ? part.getId() : "") + "/" + bone;

                Matrix4f captured = PovHandMatrices.getFull(key);
                if (captured == null && !fallbackKey1.equals(key)) captured = PovHandMatrices.getFull(fallbackKey1);
                if (captured == null && !fallbackKey2.equals(key)) captured = PovHandMatrices.getFull(fallbackKey2);

                Transform transform = transformEditor.getTransform();
                Transform baseline = transform.copy();
                Matrix4f bodyPartBase = UIPovHandEditor.getBodyPartBase(part);

                Matrix4f evaluated = PovHandMatrices.evaluateFull(key, baseline, transform);
                if (evaluated == null && !fallbackKey1.equals(key)) evaluated = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                if (evaluated == null && !fallbackKey2.equals(key)) evaluated = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                if (evaluated == null)
                {
                    evaluated = UIPovHandEditor.evaluateBodyPartBoneMatrix(bodyPartModelForm, bone, baseline, transform, bodyPartBase);
                }

                if (evaluated != null)
                {
                    captured = evaluated;
                }

                Camera camera = this.renderer.camera;
                Area viewport = handEditor.getGapArea();
                Vector3d dragOrigin = captured == null
                    ? new Vector3d(0D, 0D, -1D)
                    : new Vector3d(captured.getTranslation(new Vector3f()));

                GizmoDrag drag = new PovBodyPartGizmoDrag().setup(camera, viewport, dragOrigin);
                bbsPov$configureBodyPartDrag(drag, dragOrigin);

                Matrix4f fallbackMatrix = captured == null
                    ? new Matrix4f().translation(0F, 0F, -1F)
                    : new Matrix4f(captured);

                Supplier<Matrix4f> matrix = () ->
                {
                    Matrix4f value = PovHandMatrices.evaluateFull(key, baseline, transform);
                    if (value == null && !fallbackKey1.equals(key)) value = PovHandMatrices.evaluateFull(fallbackKey1, baseline, transform);
                    if (value == null && !fallbackKey2.equals(key)) value = PovHandMatrices.evaluateFull(fallbackKey2, baseline, transform);
                    if (value == null)
                    {
                        value = UIPovHandEditor.evaluateBodyPartBoneMatrix(bodyPartModelForm, bone, baseline, transform, bodyPartBase);
                    }
                    return value == null ? new Matrix4f(fallbackMatrix) : value;
                };

                TransformSpace space = modelForm.getGizmoSpace();
                Matrix3f displayedBasis = PovHandMatrices.getBasisForSpace(key, space);
                if (displayedBasis == null && !fallbackKey1.equals(key)) displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey1, space);
                if (displayedBasis == null && !fallbackKey2.equals(key)) displayedBasis = PovHandMatrices.getBasisForSpace(fallbackKey2, space);

                Matrix3f globalBasis = PovHandMatrices.getGlobalBasis(key);
                if (globalBasis == null && !fallbackKey1.equals(key)) globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey1);
                if (globalBasis == null && !fallbackKey2.equals(key)) globalBasis = PovHandMatrices.getGlobalBasis(fallbackKey2);

                if (displayedBasis != null)
                {
                    drag.gizmoWorldAxes.set(displayedBasis);
                }
                if (globalBasis != null)
                {
                    drag.setGlobalAxes(globalBasis);
                }

                drag.setRotateAxes(GizmoDrag.computeRotateAxes(transform, matrix));
                drag.setJacobian(GizmoDrag.computeTranslateJacobian(
                    transform,
                    () -> matrix.get().getTranslation(new Vector3f())));

                Matrix4f localMat = PovHandMatrices.getForSpace(key, TransformSpace.LOCAL);
                if (localMat == null && !fallbackKey1.equals(key)) localMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.LOCAL);
                if (localMat == null && !fallbackKey2.equals(key)) localMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.LOCAL);
                if (localMat == null) localMat = UIPovHandEditor.getBodyPartBoneMatrix(bodyPartModelForm, bone, true, bodyPartBase);

                Matrix4f parentMat = PovHandMatrices.getForSpace(key, TransformSpace.PARENT);
                if (parentMat == null && !fallbackKey1.equals(key)) parentMat = PovHandMatrices.getForSpace(fallbackKey1, TransformSpace.PARENT);
                if (parentMat == null && !fallbackKey2.equals(key)) parentMat = PovHandMatrices.getForSpace(fallbackKey2, TransformSpace.PARENT);
                if (parentMat == null) parentMat = UIPovHandEditor.getBodyPartBoneMatrix(bodyPartModelForm, bone, false, bodyPartBase);

                drag.setFrameAxes(localMat, parentMat);

                info.setReturnValue(drag);
            }
        }
    }

    /**
     * Edit-mode POV bodyparts are drawn in view-space. The drag
     * ray stays in the rendered view-space so dragging horizontally across
     * the screen moves purely across screen X without tilting into/out of depth Z.
     */
    @Unique
    private static void bbsPov$configureBodyPartDrag(GizmoDrag drag, Vector3d origin)
    {
        /* Captured POV matrices are view-space. */
        drag.view.identity();
        drag.cameraOrigin.set(0D, 0D, 0D);
        drag.gizmoOrigin.set(origin);
    }

    @Inject(method = "getOrigin(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void bbsPov$getBodyPartGizmoOrigin(float transition, CallbackInfoReturnable<Matrix4f> info)
    {
        if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null)
        {
            mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry entry = this.formsList.getCurrentFirst();
            if (entry != null && entry.part != null)
            {
                Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);
                info.setReturnValue(new Matrix4f(base));
            }
        }
    }

    @Inject(method = "getOriginMatrix(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void bbsPov$getBodyPartGizmoOriginMatrix(float transition, CallbackInfoReturnable<Matrix4f> info)
    {
        if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null)
        {
            mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry entry = this.formsList.getCurrentFirst();
            if (entry != null && entry.part != null)
            {
                Matrix4f base = UIPovHandEditor.getBodyPartBase(entry.part);
                info.setReturnValue(new Matrix4f(base));
            }
        }
    }

    @Inject(method = "getParentOriginMatrix(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void bbsPov$getBodyPartGizmoParentOriginMatrix(float transition, CallbackInfoReturnable<Matrix4f> info)
    {
        if (UIPovHandEditor.isActive() && this.isBodyPartGizmoMode() && this.formsList != null)
        {
            mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry entry = this.formsList.getCurrentFirst();
            if (entry != null && entry.part != null)
            {
                Matrix4f parent = UIPovHandEditor.getBodyPartParent(entry.part);
                info.setReturnValue(new Matrix4f(parent));
            }
        }
    }
}
