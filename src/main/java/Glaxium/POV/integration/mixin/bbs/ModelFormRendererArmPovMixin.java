package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.hand.playback.PovHandPlayback;
import Glaxium.POV.hand.render.PovHandMatrices;
import Glaxium.POV.hand.editor.PovHandPicking;
import Glaxium.POV.bodypart.PovBodyPartRenderer;
import Glaxium.POV.replay.PovReplaySettings;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.joml.Matrix4f;

/** BBS renderArm resets the model but does not reapply ModelForm.pose like render3D does. */
@Mixin(value = ModelFormRenderer.class, remap = false)
public abstract class ModelFormRendererArmPovMixin extends mchorse.bbs_mod.forms.renderers.FormRenderer<mchorse.bbs_mod.forms.forms.ModelForm> implements PovBodyPartRenderer
{
    public ModelFormRendererArmPovMixin(mchorse.bbs_mod.forms.forms.ModelForm form)
    {
        super(form);
    }

    @Shadow private MatrixCache bones;
    @Shadow private IEntity entity;
    @Unique private ModelInstance bbsPov$armModel;
    @Unique private Map<ModelGroup, Boolean> bbsPov$armVisibility;
    @Unique private Map<BOBJBone, Boolean> bbsPov$armBobjVisibility;
    @Unique private Matrix4f bbsPov$armRenderBase;
    /** Matrix already present on the POV Replay Editor's viewport stack.
     * Bone origins consumed by UIFormEditor must be relative to this matrix,
     * because UIPickableFormRenderer applies it again when drawing the gizmo. */
    @Unique private Matrix4f bbsPov$editorSceneBase;
    @Unique private Hand bbsPov$renderedHand;
    @Unique private float bbsPov$renderTransition;

    @Inject(method = "renderFirstPersonHand", at = @At("HEAD"), cancellable = true)
    private void bbsPov$captureModelState(
        MatrixStack matrices,
        int light,
        Hand hand,
        CallbackInfoReturnable<Boolean> info)
    {
        ((ModelFormRenderer) (Object) this).ensureAnimator(0F);

        if (PovHandPlayback.isActive() && !PovHandPlayback.shouldRenderModelHand(hand))
        {
            /* Report the arm as handled so BBS also suppresses the vanilla skin
             * arm, while HeldItemRenderer remains free to draw the held item. */
            info.setReturnValue(true);
            return;
        }

        ModelInstance instance = ((ModelFormRenderer) (Object) this).getModel();

        if (instance == null || instance.getModel() == null)
        {
            return;
        }

        this.bbsPov$armModel = instance;
        this.bbsPov$renderedHand = hand;
        this.bbsPov$armVisibility = new IdentityHashMap<>();
        this.bbsPov$armBobjVisibility = new IdentityHashMap<>();

        for (ModelGroup group : instance.getModel().getAllGroups())
        {
            this.bbsPov$armVisibility.put(group, group.visible);
        }

        if (instance.getModel() instanceof BOBJModel bobjModel)
        {
            for (BOBJBone bone : bobjModel.getAllBOBJBones())
            {
                this.bbsPov$armBobjVisibility.put(bone, bone.visible);
            }

            ArmorSlot slot = hand == Hand.MAIN_HAND ? instance.getFpMain() : instance.getFpOffhand();
            if (slot != null && slot.group != null && !slot.group.isBlank())
            {
                for (BOBJBone bone : bobjModel.getAllBOBJBones())
                {
                    bone.visible = bbsPov$belongsToSlot(bone, slot.group);
                }
            }
        }
    }

    @Inject(
        method = "renderFirstPersonHand",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel",
            shift = At.Shift.BEFORE))
    private void bbsPov$captureRenderBase(
        MatrixStack matrices,
        int light,
        Hand hand,
        CallbackInfoReturnable<Boolean> info)
    {
        if ((PovHandPlayback.isActive() || Glaxium.POV.editor.UIPovHandEditor.isActive()) && !PovHandPicking.isStencilPass())
        {
            this.bbsPov$armRenderBase = new Matrix4f(matrices.peek().getPositionMatrix());
        }
    }

    /** BBS applies IK, embedded model physics and constraints inside renderModel().
     * Apply the replay pose after those systems, immediately before the model is
     * submitted, so no live animation can overwrite the POV keyframes. */
    @Inject(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/cubic/ModelInstance;render",
            shift = At.Shift.BEFORE))
    private void bbsPov$applyFinalHandPose(
        IEntity entity,
        Supplier<ShaderProgram> shader,
        MatrixStack matrices,
        ModelInstance instance,
        int light,
        int overlay,
        Color color,
        Color formColor,
        boolean additive,
        StencilMap stencilMap,
        float transition,
        MatrixStack world,
        CallbackInfo info)
    {
        this.bbsPov$renderTransition = transition;

        boolean matched = instance == this.bbsPov$armModel;

        if (!matched)
        {
            return;
        }

        IModel model = instance.getModel();

        if (model != null)
        {
            if (PovHandPlayback.isActive() || Glaxium.POV.editor.UIPovHandEditor.isActive())
            {
                /* Erase animator/IK/physics output. With no POV pose keys this leaves
                 * the model in its authored first-person rest pose. */
                model.resetPose();
                Pose pose = PovHandPlayback.getRenderPose();

                if (pose == null)
                {
                    pose = ((ModelFormRenderer) (Object) this).getPose();
                }

                if (pose != null)
                {
                    model.applyPose(pose);
                }
            }

            if (model instanceof BOBJModel bobjModel && this.bbsPov$renderedHand != null)
            {
                ArmorSlot slot = this.bbsPov$renderedHand == Hand.MAIN_HAND ? instance.getFpMain() : instance.getFpOffhand();
                if (slot != null && slot.group != null && !slot.group.isBlank())
                {
                    for (BOBJBone bone : bobjModel.getAllBOBJBones())
                    {
                        bone.visible = bbsPov$belongsToSlot(bone, slot.group);
                    }
                }
            }
        }
    }

    @Unique
    private FormRenderingContext bbsPov$activeFormContext;
    @Unique
    private int bbsPov$activeBaseTarget;

    @ModifyArg(
        method = "renderFirstPersonHand",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel"),
        index = 9)
    private StencilMap bbsPov$renderHandStencil(StencilMap original)
    {
        if (this.bbsPov$activeFormContext != null && this.bbsPov$activeFormContext.isPicking()
            && this.bbsPov$activeFormContext.stencilMap != null)
        {
            return this.bbsPov$activeFormContext.stencilMap;
        }

        StencilMap map = PovHandPicking.getStencilMap();

        if (PovHandPicking.isStencilPass() && map != null)
        {
            ModelFormRenderer renderer = (ModelFormRenderer) (Object) this;
            ModelInstance instance = renderer.getModel();

            if (instance != null && instance.getModel() != null
                && PovHandPicking.beginModelMapping(instance))
            {
                instance.fillStencilMap(map, renderer.getForm());
            }

            return map;
        }

        return original;
    }

    @ModifyArg(
        method = "renderFirstPersonHand",
        at = @At(
            value = "INVOKE",
            target = "Lmchorse/bbs_mod/forms/renderers/ModelFormRenderer;renderModel"),
        index = 1)
    private Supplier<ShaderProgram> bbsPov$useNativePickerShader(Supplier<ShaderProgram> original)
    {
        if (this.bbsPov$activeFormContext != null && this.bbsPov$activeFormContext.isPicking())
        {
            ShaderProgram program = BBSShaders.getPickerModelsProgram();
            GlUniform target = program.getUniform("Target");

            if (target != null)
            {
                target.set(this.bbsPov$activeBaseTarget);
            }

            return () -> program;
        }

        return PovHandPicking.isStencilPass() ? ModelFormRendererArmPovMixin::bbsPov$getPickerShader : original;
    }

    @Unique
    private static ShaderProgram bbsPov$getPickerShader()
    {
        ShaderProgram program = BBSShaders.getPickerModelsProgram();
        GlUniform target = program.getUniform("Target");

        if (target != null)
        {
            target.set(PovHandPicking.getStencilTarget());
        }

        return program;
    }

    @Inject(method = "renderFirstPersonHand", at = @At("RETURN"))
    private void bbsPov$restoreModelState(
        MatrixStack matrices,
        int light,
        Hand hand,
        CallbackInfoReturnable<Boolean> info)
    {
        ModelInstance instance = this.bbsPov$armModel;
        Map<ModelGroup, Boolean> visibility = this.bbsPov$armVisibility;
        Map<BOBJBone, Boolean> bobjVisibility = this.bbsPov$armBobjVisibility;

        if (instance != null && this.bbsPov$armRenderBase != null
            && this.bbsPov$renderedHand != null && !PovHandPicking.isStencilPass()
            && (this.bbsPov$activeFormContext == null || !this.bbsPov$activeFormContext.isPicking())
            && (PovHandPlayback.isActive() || Glaxium.POV.editor.UIPovHandEditor.isActive()))
        {
            MatrixCache cache = new MatrixCache();
            instance.captureMatrices(cache);

            Matrix4f renderBase = this.bbsPov$editorSceneBase != null
                ? new Matrix4f(this.bbsPov$editorSceneBase).invert().mul(this.bbsPov$armRenderBase)
                : this.bbsPov$armRenderBase;

            PovHandMatrices.capture(
                instance,
                this.bbsPov$renderedHand == Hand.MAIN_HAND
                    ? instance.getFpMain()
                    : instance.getFpOffhand(),
                renderBase,
                cache,
                ((ModelFormRenderer) (Object) this).getPose());
        }

        this.bbsPov$armModel = null;
        this.bbsPov$armVisibility = null;
        this.bbsPov$armBobjVisibility = null;
        this.bbsPov$armRenderBase = null;
        this.bbsPov$renderedHand = null;

        if (instance != null)
        {
            if (visibility != null)
            {
                for (Map.Entry<ModelGroup, Boolean> entry : visibility.entrySet())
                {
                    entry.getKey().visible = entry.getValue();
                }
            }
            if (bobjVisibility != null)
            {
                for (Map.Entry<BOBJBone, Boolean> entry : bobjVisibility.entrySet())
                {
                    entry.getKey().visible = entry.getValue();
                }
            }
        }
    }

    @Unique
    private static boolean bbsPov$belongsToSlot(BOBJBone bone, String slotGroup)
    {
        for (BOBJBone current = bone; current != null; current = current.parentBone)
        {
            if (slotGroup.equals(current.name))
            {
                return true;
            }
        }

        return false;
    }

    @Inject(method = "render3D", at = @At("HEAD"), cancellable = true)
    private void bbsPov$renderBothArmsInHandEditor(FormRenderingContext context, CallbackInfo info)
    {
        if (Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            Glaxium.POV.editor.UIPovHandEditor editor = Glaxium.POV.editor.UIPovHandEditor.getActive();
            if (editor == null || editor.getRootForm() == null || ((ModelFormRenderer) (Object) this).getForm() != editor.getRootForm())
            {
                return;
            }

            info.cancel();

            /* UIModelRenderer has already placed its camera/view matrix on the
             * stack. Keep it out of the captured bone matrices, otherwise the
             * POV gizmo applies that view twice and appears on the opposite
             * side of the selected body/bone. */
            this.bbsPov$editorSceneBase = new Matrix4f(context.stack.peek().getPositionMatrix());

            ModelFormRenderer self = (ModelFormRenderer) (Object) this;
            /* Freeze animations in the editor — they should only advance when the
             * film timeline is playing/scrubbed, not in real-time on every frame.
             * Mirrors PovHandPlayback which also calls ensureAnimator(0F). */
            self.ensureAnimator(0F);

            this.bbsPov$activeFormContext = context;
            this.bbsPov$activeBaseTarget = (context.isPicking() && context.stencilMap != null) ? context.getPickingIndex() : 0;
            boolean prevSuppress = PovHandPlayback.suppressFormTransform;
            PovHandPlayback.suppressFormTransform = true;
            try
            {
                bbsPov$renderFirstPersonArm(context, Hand.OFF_HAND);
                bbsPov$renderFirstPersonArm(context, Hand.MAIN_HAND);

                if (context.stencilMap != null)
                {
                    ModelInstance model = self.getModel();
                    if (model != null)
                    {
                        model.fillStencilMap(context.stencilMap, self.getForm());
                    }
                }

                if (self.getForm() != null && self.getForm().parts != null)
                {
                    int partIndex = 0;
                    for (mchorse.bbs_mod.forms.forms.BodyPart part : self.getForm().parts.getAllTyped())
                    {
                        if (part.getForm() == null)
                        {
                            partIndex++;
                            continue;
                        }

                        context.stack.push();
                        if (context.world != null)
                        {
                            context.world.push();
                        }

                        String bone = part.bone.get();
                        if (bone != null && !bone.isEmpty())
                        {
                            Matrix4f boneMat = part.filterBoneMatrix(PovHandMatrices.getFull(bone));
                            if (boneMat != null)
                            {
                                mchorse.bbs_mod.utils.MatrixStackUtils.multiply(context.stack, boneMat);
                                if (context.world != null)
                                {
                                    mchorse.bbs_mod.utils.MatrixStackUtils.multiply(context.world, boneMat);
                                }
                            }
                            else
                            {
                                context.stack.multiply(RotationAxis.POSITIVE_Y.rotation(mchorse.bbs_mod.utils.MathUtils.PI));
                                if (context.world != null)
                                {
                                    context.world.multiply(RotationAxis.POSITIVE_Y.rotation(mchorse.bbs_mod.utils.MathUtils.PI));
                                }
                            }
                        }
                        else
                        {
                            /* Attached directly to camera: centered in view, upright, facing the camera */
                            context.stack.translate(0.0F, -0.75F, -1.2F);
                            context.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                            if (context.world != null)
                            {
                                context.world.translate(0.0F, -0.75F, -1.2F);
                                context.world.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                            }
                        }

                        this.renderBodyPart(part, context);

                        if (part.getForm() instanceof ModelForm bodyPartModelForm
                            && FormUtilsClient.getRenderer(part.getForm()) instanceof ModelFormRenderer modelRenderer)
                        {
                            MatrixStack attachment = new MatrixStack();
                            attachment.loadIdentity();
                            if (bone == null || bone.isBlank())
                            {
                                attachment.translate(0F, -0.75F, -1.2F);
                                attachment.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                            }
                            else
                            {
                                Matrix4f parent = PovHandMatrices.getFull(bone);
                                if (parent != null)
                                {
                                    mchorse.bbs_mod.utils.MatrixStackUtils.multiply(attachment, parent);
                                }
                            }
                            String formPath = mchorse.bbs_mod.forms.FormUtils.getPath(part.getForm());
                            PovHandMatrices.captureBodyPart(
                                formPath,
                                modelRenderer,
                                part.getRenderEntity(this.entity),
                                attachment,
                                part.transform.get());
                            if (partIndex >= 0 && !String.valueOf(partIndex).equals(formPath))
                            {
                                PovHandMatrices.captureBodyPart(
                                    String.valueOf(partIndex),
                                    modelRenderer,
                                    part.getRenderEntity(this.entity),
                                    attachment,
                                    part.transform.get());
                            }
                            if (part.getId() != null && !part.getId().equals(formPath) && !part.getId().equals(String.valueOf(partIndex)))
                            {
                                PovHandMatrices.captureBodyPart(
                                    part.getId(),
                                    modelRenderer,
                                    part.getRenderEntity(this.entity),
                                    attachment,
                                    part.transform.get());
                            }
                        }

                        context.stack.pop();
                        if (context.world != null)
                        {
                            context.world.pop();
                        }
                        partIndex++;
                    }
                }
            }
            finally
            {
                this.bbsPov$activeFormContext = null;
                this.bbsPov$editorSceneBase = null;
                PovHandPlayback.suppressFormTransform = prevSuppress;
            }
        }
    }

    @Inject(method = "renderBodyParts", at = @At("HEAD"), cancellable = true)
    private void bbsPov$cancelNativeBodyPartsInHandEditor(FormRenderingContext context, CallbackInfo info)
    {
        if (Glaxium.POV.editor.UIPovHandEditor.isActive())
        {
            Glaxium.POV.editor.UIPovHandEditor editor = Glaxium.POV.editor.UIPovHandEditor.getActive();
            if (editor != null && editor.getRootForm() != null && ((ModelFormRenderer) (Object) this).getForm() == editor.getRootForm())
            {
                if (this.bones != null)
                {
                    this.bones.clear();
                }
                info.cancel();
            }
        }
    }

    @Unique
    private void bbsPov$renderFirstPersonArm(FormRenderingContext context, Hand hand)
    {
        MatrixStack matrices = context.stack;
        matrices.push();

        boolean isRight = hand == Hand.MAIN_HAND;
        float f = isRight ? 1.0f : -1.0f;

        matrices.translate(f * 0.64000005f, -0.6f, -0.71999997f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0f));
        matrices.translate(f * -1.0f, 3.6f, 3.5f);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0f));
        matrices.translate(f * 5.6f, 0.0f, 0.0f);

        ((ModelFormRenderer) (Object) this).renderArm(matrices, LightmapTextureManager.pack(15, 15), null, hand);

        matrices.pop();
    }

    @Override
    public void bbsPov$renderBodyParts(int light, StencilMap stencilMap)
    {
        if (Glaxium.POV.editor.UIPovHandEditor.isActive()
            || this.form == null || this.form.parts == null)
        {
            return;
        }

        int partIndex = 0;
        boolean prevSuppress = PovHandPlayback.suppressFormTransform;
        PovHandPlayback.suppressFormTransform = true;

        try
        {
            for (mchorse.bbs_mod.forms.forms.BodyPart part : this.form.parts.getAllTyped())
            {
                Form partForm = part.getForm();

                if (partForm == null || !partForm.visible.get())
                {
                    partIndex++;
                    continue;
                }

                MatrixStack stack = new MatrixStack();
                String bone = part.bone.get();

                if (bone == null || bone.isBlank())
                {
                    /* Empty bone has POV-specific Camera attachment semantics. */
                    stack.translate(0F, -0.75F, -1.2F);
                    stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                }
                else
                {
                    /* Draw only after both hands have completed.  Rendering a nested
                     * form from inside renderArm() made it inherit the hand renderer's
                     * temporary group visibility (especially when both forms shared
                     * one model instance), which made full-model bodyparts disappear.
                     * PovHandMatrices is captured from that exact visible arm pass. */
                    Matrix4f parent = PovHandMatrices.getFull(bone);

                    if (parent == null)
                    {
                        partIndex++;
                        continue;
                    }

                    mchorse.bbs_mod.utils.MatrixStackUtils.multiply(stack, parent);
                }

                float transition = this.bbsPov$renderTransition;
                if (transition == 0F)
                {
                    transition = PovHandPlayback.getActiveTransition();
                }

                FormRenderingContext context = new FormRenderingContext()
                    .set(mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_FP,
                        this.entity,
                        stack,
                        light,
                        net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                        transition)
                    .stencilMap(stencilMap);

                /* [PORTING NOTE: BBS FILM STENCIL PICKING FIX]
                 * When stencilMap is non-null, register partForm -> partIndex with PovHandPicking.
                 * DO NOT call partInstance.fillStencilMap(stencilMap, bodyModel) here manually!
                 * BBS's FormRenderer.render() -> this.updateStencilMap() already invokes
                 * model.fillStencilMap() during renderBodyPart(part, context). Calling it manually
                 * beforehand increments stencilMap.objectIndex prematurely and desynchronizes
                 * the shader target colors from the stencil index map, breaking picking. */
                if (stencilMap != null)
                {
                    PovHandPicking.registerBodyPart(partForm, partIndex);
                }

                // Held-item layers can leave depth testing disabled. Bodyparts use
                // direct model drawing and must establish their own depth state.
                boolean depthTest = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                boolean depthWrite = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
                int depthFunction = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_DEPTH_FUNC);
                try
                {
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthMask(true);
                    RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
                    this.renderBodyPart(part, context);
                }
                finally
                {
                    RenderSystem.depthFunc(depthFunction == org.lwjgl.opengl.GL11.GL_ALWAYS ? org.lwjgl.opengl.GL11.GL_LEQUAL : depthFunction);
                    RenderSystem.depthMask(depthWrite);
                    if (depthTest)
                    {
                        RenderSystem.enableDepthTest();
                    }
                    else
                    {
                        RenderSystem.disableDepthTest();
                    }
                }

                partIndex++;
            }

            if (stencilMap == null)
            {
                this.bbsPov$captureBodyPartRecursive(this.form, this.entity, "");
            }
        }
        finally
        {
            PovHandPlayback.suppressFormTransform = prevSuppress;
        }
    }

    @Unique
    private void bbsPov$captureBodyPartRecursive(Form form, IEntity entity, String parentPath)
    {
        if (form == null || form.parts == null)
        {
            return;
        }

        for (mchorse.bbs_mod.forms.forms.BodyPart part : form.parts.getAllTyped())
        {
            Form partForm = part.getForm();
            if (partForm == null || !partForm.visible.get())
            {
                continue;
            }

            String bone = part.bone.get();
            MatrixStack attachment = new MatrixStack();
            attachment.loadIdentity();

            if (parentPath == null || parentPath.isEmpty())
            {
                if (bone == null || bone.isBlank())
                {
                    attachment.translate(0F, -0.75F, -1.2F);
                    attachment.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                }
                else
                {
                    Matrix4f parent = PovHandMatrices.getFull(bone);
                    if (parent != null)
                    {
                        mchorse.bbs_mod.utils.MatrixStackUtils.multiply(attachment, parent);
                    }
                }
            }
            else
            {
                Matrix4f parent = (bone != null && !bone.isBlank())
                    ? PovHandMatrices.getFull(parentPath + "/" + bone)
                    : PovHandMatrices.getFull(parentPath);
                if (parent == null)
                {
                    parent = PovHandMatrices.getFull(parentPath);
                }
                if (parent != null)
                {
                    mchorse.bbs_mod.utils.MatrixStackUtils.multiply(attachment, parent);
                }
            }

            String formPath = mchorse.bbs_mod.forms.FormUtils.getPath(partForm);

            if (partForm instanceof ModelForm
                && FormUtilsClient.getRenderer(partForm) instanceof ModelFormRenderer modelRenderer)
            {
                PovHandMatrices.captureBodyPart(
                    formPath,
                    modelRenderer,
                    part.getRenderEntity(entity),
                    attachment,
                    part.transform.get());

                mchorse.bbs_mod.utils.MatrixStackUtils.applyTransform(attachment, part.transform.get());
                this.bbsPov$captureBodyPartRecursive(partForm, entity, formPath);
            }
        }
    }
}
