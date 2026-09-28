package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import Glaxium.POV.replay.ReplayPovAccess;
import java.util.List;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.misc.TrackerFrame;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.film.PreviewHud;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import mchorse.bbs_mod.ui.utils.GizmoInteraction;
import mchorse.bbs_mod.ui.utils.context.UIChoiceMenu;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {UIFilmController.class},
   remap = false
)
public class UIFilmControllerPovMixin {
   @Shadow
   public UIFilmPanel panel;
   @Shadow
   private GizmoInteraction gizmo;

   @ModifyConstant(
      method = {"setPov"},
      constant = {@Constant(
         intValue = 6
      )}
   )
   private int bbsPov$includePovInModeCount(int original) {
      return 7;
   }

   @ModifyVariable(
      method = {"handleFirstThirdPerson"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 3
   )
   private int bbsPov$useFirstPersonCamera(int mode) {
      return mode == 6 ? 3 : mode;
   }

   @Inject(
      method = {"handleFirstThirdPerson"},
      at = {@At("TAIL")}
   )
   private void bbsPov$applyHardcoreLookInPovMode(Camera camera, float transition, int mode, CallbackInfo info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (controller.getPovMode() == 6
         && this.panel.replayEditor.getReplay() instanceof ReplayPovAccess access
         && (Boolean)access.bbsPov$getHardcoreLook().get()) {
         IEntity entity = controller.getCurrentEntity();
         if (entity != null) {
            float t = controller.isPlaying() ? transition : 0.0F;
            this.bbsPov$applyHardcoreCamera(camera, controller, entity, t);
         }
      }
   }

   private void bbsPov$applyHardcoreCamera(Camera camera, UIFilmController controller, IEntity entity, float transition) {
      Form form = entity.getForm();
      if (form != null) {
         FormRenderer formRenderer = FormUtilsClient.getRenderer(form);
         if (formRenderer instanceof ModelFormRenderer modelFormRenderer) {
            modelFormRenderer.ensureAnimator(transition);
         }

         String headBone = "head";
         if (formRenderer != null) {
            MatrixCache map = formRenderer.collectMatrices(entity, transition);
            if (!map.has(headBone)) {
               for (String key : map.keySet()) {
                  if (key.equalsIgnoreCase("head") || key.toLowerCase().endsWith("/head") || key.toLowerCase().endsWith(".head")) {
                     headBone = key;
                     break;
                  }
               }
            }

            if (!map.has(headBone)) {
               for (Object boneObj : formRenderer.getBones()) {
                  String bone = String.valueOf(boneObj);
                  if (bone.equalsIgnoreCase("head") || bone.toLowerCase().endsWith("head")) {
                     headBone = bone;
                     break;
                  }
               }
            }
         }

         TrackerFrame frame = TrackerFrame.resolve(
            controller.getEntities(), entity, headBone, camera.position.x, camera.position.y, camera.position.z, transition
         );
         if (frame != null) {
            double scaleY = 1.0;
            if (form.transform != null && form.transform.get() != null) {
               scaleY = Math.max(0.001, (double)((Transform)form.transform.get()).scale.y);
            }

            double eyeDiff = (entity.getEyeHeight() - 1.5) / scaleY;
            Point eyeOffset = new Point(0.0, eyeDiff, 0.0);
            Vector3d headPos = frame.position(eyeOffset);
            Angle headAngle = frame.angles(new Point(0.0, 0.0, 0.0));
            camera.position.set(headPos.x, headPos.y, headPos.z);
            camera.rotation
               .set((float)Math.toRadians((double)headAngle.pitch), (float)Math.toRadians((double)headAngle.yaw), (float)Math.toRadians((double)headAngle.roll));
         }
      }
   }

   @Redirect(
      method = {"renderFrame"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/film/Recorder;renderCameraPreview(Lmchorse/bbs_mod/camera/values/Position;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/util/math/MatrixStack;)V"
      )
   )
   private void bbsPov$hideRecordingCameraPreview(Position position, net.minecraft.client.render.Camera camera, MatrixStack matrices) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (controller.getPovMode() != 6) {
         Recorder.renderCameraPreview(position, camera, matrices);
      }
   }

   @Inject(
      method = {"getOrbitModeIcon(I)Lmchorse/bbs_mod/ui/utils/icons/Icon;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$getPovIcon(int mode, CallbackInfoReturnable<Icon> info) {
      if (mode == 6) {
         info.setReturnValue(Icons.VISIBLE);
      }
   }

   @Inject(
      method = {"getOrbitModeLabel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void bbsPov$getPovLabel(int mode, CallbackInfoReturnable<IKey> info) {
      if (mode == 6) {
         info.setReturnValue(IKey.constant("POV"));
      }
   }

   @Inject(
      method = {"getGizmoProjection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$useHandGizmoProjection(CallbackInfoReturnable<Matrix4f> info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      Matrix4f projection = PovHandPicking.getProjection();
      if (controller.getPovMode() == 6
         && this.panel instanceof UIFilmPanelPovAccess access
         && access.bbsPov$getEditor() != null
         && access.bbsPov$getEditor().isPoseGizmoSection()
         && projection != null) {
         info.setReturnValue(projection);
      }
   }

   @Inject(
      method = {"getBone"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$usePovEditorBone(CallbackInfoReturnable<Pair<String, TransformSpace>> info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (controller.getPovMode() == 6
         && this.panel instanceof UIFilmPanelPovAccess access
         && access.bbsPov$getEditor() != null
         && access.bbsPov$getEditor().isPoseGizmoSection()) {
         info.setReturnValue(access.bbsPov$getEditor().keyframeEditor.getBone());
      }
   }

   @Inject(
      method = {"renderHUD"},
      at = {@At("HEAD")}
   )
   private void bbsPov$refreshVisibleGizmo(UIContext context, PreviewHud hud, Area area, CallbackInfo info) {
      if (((UIFilmController)(Object)this).getPovMode() == 6) {
         PovHandGizmo.captureVisual();
      }
   }

   @Inject(
      method = {"canShowGizmo"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void bbsPov$isolateGizmoInPovCamera(CallbackInfoReturnable<Boolean> info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (controller.getPovMode() == 6) {
         info.setReturnValue(UIBaseMenu.shouldRenderAxes() && !controller.isRecording() && this.bbsPov$getHandGizmoTransform() != null);
      }
   }

   @Inject(
      method = {"setPov"},
      at = {@At("HEAD")}
   )
   private void bbsPov$stopOldGizmoWhenCameraChanges(int mode, CallbackInfo info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (mode == 6 || controller.getPovMode() == 6) {
         controller.stopGizmoInteraction();
         Gizmo.INSTANCE.stop();
      }
   }

   @Redirect(
      method = {"startGizmo"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/ui/film/replays/UIReplaysEditorUtils;startFilmGizmo(Lmchorse/bbs_mod/ui/film/UIFilmPanel;Lmchorse/bbs_mod/ui/framework/UIContext;IF)Z"
      )
   )
   private boolean bbsPov$startHandGizmo(UIFilmPanel panel, UIContext context, int index, float transition) {
      UIFilmController controller = (UIFilmController)(Object)this;
      UIPropTransform transform = this.bbsPov$getHandGizmoTransform();
      if (controller.getPovMode() == 6 && transform != null) {
         GizmoDrag drag = UIReplaysEditorUtils.buildFilmGizmoDrag(panel, panel.getCamera(), panel.preview.getViewport(), transform, transition);
         return Gizmo.INSTANCE.start(index, context.mouseX, context.mouseY, transform, drag);
      } else {
         return UIReplaysEditorUtils.startFilmGizmo(panel, context, index, transition);
      }
   }

   @Redirect(
      method = {"toggleOrbitMode"},
      at = @At(
         value = "INVOKE",
         target = "Lmchorse/bbs_mod/ui/utils/context/UIChoiceMenu;of(Ljava/lang/Iterable;)Lmchorse/bbs_mod/ui/utils/context/UIChoiceMenu;"
      )
   )
   private UIChoiceMenu<Integer> bbsPov$addPovCameraMode(Iterable<Integer> modes) {
      List<Integer> list = List.of(0, 1, 2, 6);
      return UIChoiceMenu.of(list);
   }

   @ModifyVariable(
      method = {"setPov"},
      at = @At("HEAD"),
      argsOnly = true
   )
   private int bbsPov$sanitizePovMode(int pov) {
      return pov != 3 && pov != 4 && pov != 5 ? pov : 6;
   }

   @Inject(
      method = {"subMouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$pickHandBeforeReplayController(UIContext context, CallbackInfoReturnable<Boolean> info) {
      UIFilmController controller = (UIFilmController)(Object)this;
      if (controller.getPovMode() == 6 && this.bbsPov$getHandGizmoTransform() != null && context.mouseButton == 0 && controller.picker.getStencil().hasPicked()
         )
       {
         int index = controller.picker.getStencil().getIndex();
         if (index >= 1 && index <= 19 && controller.startGizmo(context, index)) {
            info.setReturnValue(true);
            return;
         }
      }

      if (controller.getPovMode() == 6 && this.bbsPov$getHandGizmoTransform() != null && this.gizmo.mouseClickedSphere(context)) {
         info.setReturnValue(true);
      } else {
         if (controller.getPovMode() == 6
            && this.panel.preview.getViewport().isInside(context)
            && this.panel instanceof UIFilmPanelPovAccess access
            && access.bbsPov$getEditor() != null
            && access.bbsPov$getEditor().pickViewport(context, this.panel.preview.getViewport())) {
            info.setReturnValue(true);
         }
      }
   }

   private UIPropTransform bbsPov$getHandGizmoTransform() {
      if (this.panel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null && access.bbsPov$getEditor().isPoseGizmoSection()) {
         return PovHandGizmo.getTransform();
      }

      return null;
   }
}
