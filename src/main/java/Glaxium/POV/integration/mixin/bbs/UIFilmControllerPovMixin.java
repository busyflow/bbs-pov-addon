package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.editor.PovHandGizmo;
import Glaxium.POV.PovAddon;
import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.GizmoDrag;
import mchorse.bbs_mod.ui.utils.GizmoInteraction;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.context.UIChoiceMenu;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.graphics.texture.Texture;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
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

@Mixin(value = UIFilmController.class, remap = false)
public class UIFilmControllerPovMixin
{
    @Shadow public UIFilmPanel panel;
    @Shadow private GizmoInteraction gizmo;

    @ModifyConstant(method = "setPov", constant = @Constant(intValue = 6))
    private int bbsPov$includePovInModeCount(int original)
    {
        return PovCameraMode.MODE_COUNT;
    }

    @ModifyVariable(method = "handleFirstThirdPerson", at = @At("HEAD"), argsOnly = true, index = 3)
    private int bbsPov$useFirstPersonCamera(int mode)
    {
        return mode == PovCameraMode.POV ? UIFilmController.CAMERA_MODE_FIRST_PERSON : mode;
    }

    @Inject(method = "handleFirstThirdPerson", at = @At("TAIL"))
    private void bbsPov$applyHardcoreLookInPovMode(
        mchorse.bbs_mod.camera.Camera camera,
        float transition,
        int mode,
        CallbackInfo info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        if (controller.getPovMode() == PovCameraMode.POV)
        {
            mchorse.bbs_mod.forms.entities.IEntity entity = controller.getCurrentEntity();

            if (entity != null)
            {
                mchorse.bbs_mod.film.replays.Replay replay = this.panel.replayEditor.getReplay();
                boolean hardcore = replay instanceof Glaxium.POV.replay.ReplayPovAccess access
                    && access.bbsPov$getHardcoreLook().get();

                boolean isRunning = controller.panel.getRunner() != null && controller.panel.getRunner().isRunning();
                if (hardcore)
                {
                    float t = isRunning ? transition : 0F;
                    this.bbsPov$applyHardcoreCamera(camera, controller, entity, t);
                }
                else
                {
                    float t = isRunning ? transition : 0F;
                    float filmTick = (float) this.panel.getCursor() + t;
                    double baseEyeHeight = entity.getEyeHeight();
                    double povEyeHeight = PovCameraClips.getSmoothPovEyeHeight(replay, entity, entity.getForm(), filmTick);
                    if (Math.abs(povEyeHeight - baseEyeHeight) > 1e-5)
                    {
                        camera.position.y += (povEyeHeight - baseEyeHeight);
                    }
                }
            }
        }
    }

    private void bbsPov$applyHardcoreCamera(
        mchorse.bbs_mod.camera.Camera camera,
        UIFilmController controller,
        mchorse.bbs_mod.forms.entities.IEntity entity,
        float transition)
    {
        mchorse.bbs_mod.forms.forms.Form form = entity.getForm();
        if (form == null)
        {
            return;
        }

        mchorse.bbs_mod.forms.renderers.FormRenderer formRenderer = mchorse.bbs_mod.forms.FormUtilsClient.getRenderer(form);
        if (formRenderer instanceof mchorse.bbs_mod.forms.renderers.ModelFormRenderer modelFormRenderer)
        {
            modelFormRenderer.ensureAnimator(transition);
        }

        String headBone = "head";
        if (formRenderer != null)
        {
            mchorse.bbs_mod.forms.renderers.utils.MatrixCache map = formRenderer.collectMatrices(entity, transition);
            if (!map.has(headBone))
            {
                for (String key : map.keySet())
                {
                    if (key.equalsIgnoreCase("head") || key.toLowerCase().endsWith("/head") || key.toLowerCase().endsWith(".head"))
                    {
                        headBone = key;
                        break;
                    }
                }
            }

            if (!map.has(headBone))
            {
                java.util.List<String> bones = formRenderer.getBones();
                for (String bone : bones)
                {
                    if (bone.equalsIgnoreCase("head") || bone.toLowerCase().endsWith("head"))
                    {
                        headBone = bone;
                        break;
                    }
                }
            }
        }

        mchorse.bbs_mod.camera.clips.misc.TrackerFrame frame = mchorse.bbs_mod.camera.clips.misc.TrackerFrame.resolve(
            controller.getEntities(),
            entity,
            headBone,
            camera.position.x,
            camera.position.y,
            camera.position.z,
            transition
        );

        if (frame != null)
        {
            double scaleY = 1.0D;
            if (form.transform != null && form.transform.get() != null)
            {
                scaleY = Math.max(0.001D, form.transform.get().scale.y);
            }

            float filmTick = (float) this.panel.getCursor() + transition;
            mchorse.bbs_mod.film.replays.Replay replay = this.panel.replayEditor.getReplay();
            double eyeHeight = PovCameraClips.getSmoothPovEyeHeight(replay, entity, form, filmTick);
            double eyeDiff = (eyeHeight - 1.5D) / scaleY;
            mchorse.bbs_mod.camera.data.Point eyeOffset = new mchorse.bbs_mod.camera.data.Point(0D, eyeDiff, 0D);

            org.joml.Vector3d headPos = frame.position(eyeOffset);
            mchorse.bbs_mod.camera.data.Angle headAngle = frame.angles(new mchorse.bbs_mod.camera.data.Point(0, 0, 0));

            camera.position.set(headPos.x, headPos.y, headPos.z);
            camera.rotation.set(
                (float) Math.toRadians(headAngle.pitch),
                (float) Math.toRadians(headAngle.yaw),
                (float) Math.toRadians(headAngle.roll)
            );
        }
    }

    @Redirect(
        method = "renderFrame",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/film/Recorder;renderCameraPreview"))
    private void bbsPov$hideRecordingCameraPreview(
        Position position,
        net.minecraft.client.render.Camera camera,
        MatrixStack matrices)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        if (controller.getPovMode() != PovCameraMode.POV)
        {
            Recorder.renderCameraPreview(position, camera, matrices);
        }
    }


    @Inject(method = "getOrbitModeIcon(I)Lmchorse/bbs_mod/ui/utils/icons/Icon;", at = @At("HEAD"), cancellable = true)
    private void bbsPov$getPovIcon(int mode, CallbackInfoReturnable<Icon> info)
    {
        if (mode == PovCameraMode.POV)
        {
            info.setReturnValue(Icons.VISIBLE);
        }
    }

    @Inject(method = "getOrbitModeLabel", at = @At("HEAD"), cancellable = true)
    private static void bbsPov$getPovLabel(int mode, CallbackInfoReturnable<IKey> info)
    {
        if (mode == PovCameraMode.POV)
        {
            info.setReturnValue(IKey.constant("POV"));
        }
    }

    @Inject(method = "getGizmoProjection", at = @At("HEAD"), cancellable = true)
    private void bbsPov$useHandGizmoProjection(CallbackInfoReturnable<Matrix4f> info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;
        Matrix4f projection = PovHandPicking.getProjection();

        if (controller.getPovMode() == PovCameraMode.POV
            && this.panel instanceof UIFilmPanelPovAccess access
            && access.bbsPov$getEditor() != null
            && access.bbsPov$getEditor().isPoseGizmoSection()
            && projection != null)
        {
            /* The arm is rendered with Minecraft's first-person projection,
             * which is deliberately independent from the Film camera FOV. */
            info.setReturnValue(projection);
        }
    }

    @Inject(method = "getBone", at = @At("HEAD"), cancellable = true)
    private void bbsPov$usePovEditorBone(CallbackInfoReturnable<Pair<String, TransformSpace>> info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        if (controller.getPovMode() == PovCameraMode.POV
            && this.panel instanceof UIFilmPanelPovAccess access
            && access.bbsPov$getEditor() != null
            && access.bbsPov$getEditor().isPoseGizmoSection())
        {
            info.setReturnValue(access.bbsPov$getEditor().keyframeEditor.getBone());
        }
    }

    /** Other form previews share Gizmo.INSTANCE. Rebind the selected POV
     * matrix immediately before drawing the handles in this viewport. The
     * projection comes from getGizmoProjection(), not the GUI projection. */
    @Inject(method = "renderHUD", at = @At("HEAD"))
    private void bbsPov$refreshVisibleGizmo(
        UIContext context,
        mchorse.bbs_mod.ui.film.PreviewHud hud,
        Area area,
        CallbackInfo info)
    {
        if (((UIFilmController) (Object) this).getPovMode() == PovCameraMode.POV)
        {
            PovHandGizmo.captureVisual();
        }
    }

    @Inject(method = "canShowGizmo", at = @At("RETURN"), cancellable = true)
    private void bbsPov$isolateGizmoInPovCamera(CallbackInfoReturnable<Boolean> info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        if (controller.getPovMode() == PovCameraMode.POV)
        {
            /* BBS's original result is replay-actor-specific and rejects the
             * custom parent Pose factory. POV owns this gate in POV mode. */
            info.setReturnValue(
                UIBaseMenu.shouldRenderAxes()
                    && !controller.isRecording()
                    && this.bbsPov$getHandGizmoTransform() != null);
        }
    }

    @Inject(method = "setPov", at = @At("HEAD"))
    private void bbsPov$stopOldGizmoWhenCameraChanges(int mode, CallbackInfo info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        if (mode == PovCameraMode.POV || controller.getPovMode() == PovCameraMode.POV)
        {
            controller.stopGizmoInteraction();
            Gizmo.INSTANCE.stop();
        }
    }

    /** Native Film gizmo startup always resolves its transform from the Replay
     * Editor. In POV mode the visible gizmo belongs to our separate editor, so
     * start the native BBS drag with that exact transform instead. */
    @Redirect(
        method = "startGizmo",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/film/replays/UIReplaysEditorUtils;startFilmGizmo(Lmchorse/bbs_mod/ui/film/UIFilmPanel;Lmchorse/bbs_mod/ui/framework/UIContext;IF)Z"))
    private boolean bbsPov$startHandGizmo(
        UIFilmPanel panel,
        UIContext context,
        int index,
        float transition)
    {
        UIFilmController controller = (UIFilmController) (Object) this;
        UIPropTransform transform = this.bbsPov$getHandGizmoTransform();

        if (controller.getPovMode() != PovCameraMode.POV || transform == null)
        {
            return UIReplaysEditorUtils.startFilmGizmo(
                panel,
                context,
                index,
                transition);
        }

        GizmoDrag drag = UIReplaysEditorUtils.buildFilmGizmoDrag(
            panel,
            panel.getCamera(),
            panel.preview.getViewport(),
            transform,
            transition);

        return Gizmo.INSTANCE.start(
            index,
            context.mouseX,
            context.mouseY,
            transform,
            drag);
    }

    @Redirect(
        method = "toggleOrbitMode",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/ui/utils/context/UIChoiceMenu;of(Ljava/lang/Iterable;)Lmchorse/bbs_mod/ui/utils/context/UIChoiceMenu;"))
    private UIChoiceMenu<Integer> bbsPov$addPovCameraMode(Iterable<Integer> modes)
    {
        List<Integer> list = List.of(
            UIFilmController.CAMERA_MODE_CAMERA,
            UIFilmController.CAMERA_MODE_FREE,
            UIFilmController.CAMERA_MODE_ORBIT,
            PovCameraMode.POV
        );
        return UIChoiceMenu.of(list);
    }

    @ModifyVariable(method = "setPov", at = @At("HEAD"), argsOnly = true)
    private int bbsPov$sanitizePovMode(int pov)
    {
        if (pov == UIFilmController.CAMERA_MODE_FIRST_PERSON
            || pov == UIFilmController.CAMERA_MODE_THIRD_PERSON_BACK
            || pov == UIFilmController.CAMERA_MODE_THIRD_PERSON_FRONT)
        {
            return PovCameraMode.POV;
        }
        return pov;
    }

    @Inject(method = "subMouseClicked", at = @At("HEAD"), cancellable = true)
    private void bbsPov$pickHandBeforeReplayController(
        UIContext context,
        CallbackInfoReturnable<Boolean> info)
    {
        UIFilmController controller = (UIFilmController) (Object) this;

        /* Handles must win over the hand stencil beneath them. Otherwise this
         * click changes the active Pose factory before BBS starts the drag. */
        if (controller.getPovMode() == PovCameraMode.POV
            && this.bbsPov$getHandGizmoTransform() != null
            && context.mouseButton == 0
            && controller.picker.getStencil().hasPicked())
        {
            int index = controller.picker.getStencil().getIndex();

            if (index >= Gizmo.STENCIL_X && index <= Gizmo.STENCIL_MAX
                && controller.startGizmo(context, index))
            {
                info.setReturnValue(true);
                return;
            }
        }

        /* The white center trackball is picked analytically by
         * GizmoInteraction, not through the numbered stencil handles above.
         * Let it claim the press before the hand/bone picker consumes it. Its
         * eventual index-16 start is redirected to the POV transform. */
        if (controller.getPovMode() == PovCameraMode.POV
            && this.bbsPov$getHandGizmoTransform() != null
            && this.gizmo.mouseClickedSphere(context))
        {
            info.setReturnValue(true);
            return;
        }

        if (controller.getPovMode() == PovCameraMode.POV
            && this.panel.preview.getViewport().isInside(context)
            && this.panel instanceof UIFilmPanelPovAccess access
            && access.bbsPov$getEditor() != null
            && access.bbsPov$getEditor().pickViewport(context, this.panel.preview.getViewport()))
        {
            info.setReturnValue(true);
        }
    }

    /**
     * Feed the selected first-person bone into BBS's real gizmo renderer. Film
     * normally does this while rendering the selected replay actor; POV hands
     * are a screen-space render and never pass through that actor code path.
     */
    private UIPropTransform bbsPov$getHandGizmoTransform()
    {
        if (!(this.panel instanceof UIFilmPanelPovAccess access)
            || access.bbsPov$getEditor() == null
            || !access.bbsPov$getEditor().isPoseGizmoSection())
        {
            return null;
        }

        return PovHandGizmo.getTransform();
    }
}
