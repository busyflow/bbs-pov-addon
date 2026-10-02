package Glaxium.POV.editor;

import Glaxium.POV.hand.editor.HandBoneHierarchy;
import Glaxium.POV.hand.editor.HandBoneUtils;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.integration.access.bbs.UIFilmPanelPovAccess;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.UIForms;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import Glaxium.POV.hand.render.PovHandMatrices;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Dedicated full-screen viewport for inspecting and editing POV hands directly
 * within the Minecraft world. Reuses the native BBS Model Editor left (Forms/Bodyparts)
 * and right (Model/Pose properties) panels while keeping the center transparent
 * to display the world and hands from the player's first-person view.
 */
public class UIPovHandEditor extends UIElement
{
    private static UIPovHandEditor active;

    private final UIFilmPanel filmPanel;
    private final UIDashboard dashboard;
    private Replay replay;
    private RecordedHandData hand;
    private UIFormPalette palette;
    private UIFormEditor formEditor;
    private String filteredModel = "";
    private Form lastConfiguredForm;
    private String lastConfiguredModel;
    private boolean configuredCamera;

    public static boolean isActive()
    {
        return active != null;
    }

    public static UIPovHandEditor getActive()
    {
        return active;
    }

    public static UIPovHandEditor open(UIFilmPanel filmPanel, Replay replay, RecordedHandData hand)
    {
        if (filmPanel == null)
        {
            return null;
        }

        UIDashboard dashboard = filmPanel.dashboard;

        if (active != null)
        {
            active.closeEditor();
        }

        if (filmPanel.getContext() != null)
        {
            filmPanel.getContext().closeContextMenu();
        }

        UIElement container = dashboard != null && dashboard.main != null
            ? dashboard.main
            : filmPanel.getRoot();

        if (container == null)
        {
            return null;
        }

        UIPovHandEditor editor = new UIPovHandEditor(filmPanel, dashboard, replay, hand);
        editor.resetFlex().relative(container).full(container);
        container.add(editor);
        editor.resize();

        active = editor;

        return editor;
    }

    public UIPovHandEditor(UIFilmPanel filmPanel, UIDashboard dashboard, Replay replay, RecordedHandData hand)
    {
        super();
        active = this;
        this.eventPropagataion(EventPropagation.BLOCK).markContainer();
        this.filmPanel = filmPanel;
        this.dashboard = dashboard;
        this.replay = replay;
        this.hand = hand;

        Form initial = hand == null ? null : hand.ensureBaseForm(replay == null ? null : replay.form.get());

        this.palette = new UIFormPalette((form) ->
        {
            if (form != null && this.hand != null)
            {
                this.hand.baseForm.set(FormUtils.copy(form));
            }
        });
        this.palette.noBackground();
        this.palette.cantExit();

        this.palette.setSelected(initial);
        this.palette.edit(true);

        this.formEditor = this.palette.editor;
        this.formEditor.resetFlex().relative(this).full(this);
        this.configureEditor();

        this.add(this.formEditor);
    }

    private void configureEditor()
    {
        if (this.formEditor == null)
        {
            return;
        }

        this.formEditor.renderer.setVisible(true);
        this.formEditor.renderer.grid = false;
        this.formEditor.renderer.updatable();

        Area gap = this.getGapArea();
        this.formEditor.renderer.area.set(gap.x, gap.y, gap.w, gap.h);
        this.formEditor.renderer.resetFlex().relative(this).xy(gap.x, gap.y).wh(gap.w, gap.h);

        this.formEditor.renderer.setPosition(0, 0, 0);
        this.formEditor.renderer.setDistance(0);
        this.formEditor.renderer.setRotation(0, 0);

        this.formEditor.icons.setVisible(false);
        this.formEditor.statesEditor.setVisible(false);
        this.formEditor.finish.setVisible(false);
        this.formEditor.openStateEditor.setVisible(false);
        this.formEditor.bodyPartEditor.useTarget.setVisible(false);
        this.formEditor.forms.x(0);

        UIForms.FormEntry currentEntry = this.formEditor.formsList != null ? this.formEditor.formsList.getCurrentFirst() : null;
        boolean isRoot = currentEntry == null || currentEntry.part == null;
        Form currentForm = isRoot ? this.getRootForm() : (currentEntry != null ? currentEntry.getForm() : this.formEditor.form);
        String currentModel = currentForm instanceof ModelForm mf ? mf.model.get() : "";

        if (this.formEditor.editor instanceof UIModelForm modelEditor && modelEditor.modelPanel != null)
        {
            if (isRoot)
            {
                if (modelEditor.modelPanel.shapeKeysSection != null)
                {
                    modelEditor.modelPanel.shapeKeysSection.removeFromParent();
                }
                if (modelEditor.modelPanel.poseEditor != null)
                {
                    if (currentForm != this.lastConfiguredForm || !java.util.Objects.equals(currentModel, this.lastConfiguredModel))
                    {
                        if (currentForm instanceof ModelForm mf)
                        {
                            filterModelPoseEditor(modelEditor.modelPanel.poseEditor, mf);
                        }
                    }
                }
            }
            else
            {
                if (modelEditor.modelPanel.poseEditor != null)
                {
                    if (currentForm != this.lastConfiguredForm || !java.util.Objects.equals(currentModel, this.lastConfiguredModel))
                    {
                        if (currentForm instanceof ModelForm mf)
                        {
                            ModelInstance instance = ModelFormRenderer.getModel(mf);
                            if (instance != null && instance.getModel() != null)
                            {
                                modelEditor.modelPanel.poseEditor.fillGroups(instance.getModel(), instance.getFlippedParts(), false, null);
                            }
                        }
                    }
                }
            }
        }

        this.lastConfiguredForm = currentForm;
        this.lastConfiguredModel = currentModel;
    }

    public static void filterModelPoseEditor(UIModelPoseEditor poseEditor, ModelForm modelForm)
    {
        if (poseEditor == null || modelForm == null)
        {
            return;
        }

        ModelInstance instance = ModelFormRenderer.getModel(modelForm);

        if (instance == null || instance.getModel() == null)
        {
            return;
        }

        HandBoneUtils.HandBones handBones = HandBoneUtils.collect(instance);

        if (handBones.depths().isEmpty())
        {
            return;
        }

        mchorse.bbs_mod.cubic.IBoneHierarchy filteredHierarchy = new HandBoneHierarchy(instance.getModel(), handBones);

        poseEditor.fillGroups(filteredHierarchy, instance.getFlippedParts(), false, null);

        String currentBone = poseEditor.getGroup();

        if (currentBone != null && !handBones.contains(currentBone))
        {
            String defaultBone = handBones.mainRoot() != null && handBones.contains(handBones.mainRoot())
                ? handBones.mainRoot()
                : handBones.depths().keySet().iterator().next();

            poseEditor.selectBone(defaultBone);
        }
    }

    public UIFilmPanel getFilmPanel()
    {
        return this.filmPanel;
    }

    public UIFormEditor getFormEditor()
    {
        return this.formEditor;
    }

    public Form getRootForm()
    {
        if (this.formEditor != null && this.formEditor.formsList != null)
        {
            java.util.List<mchorse.bbs_mod.ui.forms.editors.UIForms.FormEntry> list = this.formEditor.formsList.getList();
            if (list != null && !list.isEmpty() && list.get(0) != null && list.get(0).getForm() != null)
            {
                return list.get(0).getForm();
            }
        }
        if (this.formEditor != null && this.formEditor.form != null)
        {
            return FormUtils.getRoot(this.formEditor.form);
        }
        return this.hand != null ? this.hand.baseForm.get() : null;
    }

    public Form getPreviewForm()
    {
        return this.getRootForm();
    }

    public static BodyPart findBodyPart(Form form)
    {
        if (form == null)
        {
            return null;
        }
        UIPovHandEditor editor = getActive();
        if (editor == null)
        {
            return null;
        }
        Form root = editor.getRootForm();
        if (root == null || root.parts == null)
        {
            return null;
        }
        for (BodyPart part : root.parts.getAllTyped())
        {
            if (part.getForm() == form)
            {
                return part;
            }
        }
        return null;
    }

    public static int findBodyPartIndex(Form form)
    {
        UIPovHandEditor editor = getActive();
        if (editor == null)
        {
            return -1;
        }
        Form root = editor.getRootForm();
        if (root == null || root.parts == null)
        {
            return -1;
        }
        int index = 0;
        for (BodyPart part : root.parts.getAllTyped())
        {
            if (part.getForm() == form)
            {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static Matrix4f getBodyPartParent(BodyPart part)
    {
        Matrix4f parent = new Matrix4f();
        if (part == null)
        {
            return parent;
        }

        String bone = part.bone.get();
        if (bone != null && !bone.isEmpty())
        {
            Matrix4f boneMat = PovHandMatrices.getFull(bone);
            if (boneMat != null)
            {
                parent.set(boneMat);
            }
            else
            {
                parent.rotateY(mchorse.bbs_mod.utils.MathUtils.PI);
            }
        }
        else
        {
            parent.translate(0.0F, -0.75F, -1.2F);
            parent.rotateY((float) Math.toRadians(180.0));
        }
        return parent;
    }

    public static Matrix4f getBodyPartBase(BodyPart part)
    {
        Matrix4f base = getBodyPartParent(part);
        if (part != null && part.transform != null && part.transform.get() != null)
        {
            Matrix4f local = new Matrix4f();
            part.transform.get().setupMatrix(local);
            base.mul(local);
        }
        return base;
    }

    public static Matrix4f getBodyPartBoneMatrix(ModelForm bodyPartForm, String bone, boolean local, Matrix4f bodyPartBase)
    {
        if (bodyPartForm == null || bone == null || bone.isEmpty())
        {
            return bodyPartBase;
        }

        ModelInstance instance = ModelFormRenderer.getModel(bodyPartForm);
        if (instance == null || instance.getModel() == null)
        {
            return bodyPartBase;
        }

        MatrixCache matrices = new MatrixCache();
        instance.captureMatrices(matrices);
        MatrixCacheEntry entry = matrices.get(bone);
        if (entry != null)
        {
            Matrix4f boneMat = local ? entry.matrix() : entry.origin();
            if (boneMat != null)
            {
                /* [CRITICAL FIX: MODEL ARM MIRRORING / LEFT-RIGHT SWAP]
                 * BBS ModelFormRenderer.render3D() rotates every ModelForm by 180 degrees
                 * around Y (MathUtils.PI) before rendering its bones. In Blockbench/Cubic
                 * models, right_arm is authored at negative X and left_arm at positive X.
                 * ModelInstance.captureMatrices() returns raw local bone matrices without the
                 * 180-deg Y turn. Therefore, we MUST multiply bodyPartBase by rotateY(PI) before
                 * multiplying boneMat; otherwise, right_arm appears on the left side of the
                 * screen and left_arm on the right side. */
                return new Matrix4f(bodyPartBase)
                    .rotateY(mchorse.bbs_mod.utils.MathUtils.PI)
                    .mul(boneMat);
            }
        }
        return bodyPartBase;
    }

    public static Matrix4f evaluateBodyPartBoneMatrix(
        ModelForm bodyPartForm,
        String bone,
        Matrix4f bodyPartBase)
    {
        return getBodyPartBoneMatrix(bodyPartForm, bone, true, bodyPartBase);
    }

    public static Matrix4f evaluateBodyPartBoneMatrix(
        ModelForm bodyPartForm,
        String bone,
        Transform baseline,
        Transform edited,
        Matrix4f bodyPartBase)
    {
        if (bodyPartForm == null || bone == null || bone.isEmpty())
        {
            return bodyPartBase;
        }

        ModelInstance instance = ModelFormRenderer.getModel(bodyPartForm);
        if (instance == null || instance.getModel() == null)
        {
            return bodyPartBase;
        }

        IModel model = instance.getModel();
        Pose pose = bodyPartForm.pose.get();
        Pose originalPose = pose == null ? new Pose() : pose.copy();
        Pose working = originalPose.copy();

        if (baseline != null && edited != null)
        {
            PoseTransform transform = working.transforms.computeIfAbsent(
                bone,
                ignored -> new PoseTransform());

            transform.translate.add(
                edited.translate.x - baseline.translate.x,
                edited.translate.y - baseline.translate.y,
                edited.translate.z - baseline.translate.z);
            Transform rotationDelta = new Transform();
            rotationDelta.setModeQuaternion();
            rotationDelta.quat.set(baseline.createRotation()).invert().mul(edited.createRotation());
            transform.addRotation(rotationDelta);
        }

        try
        {
            model.resetPose();
            model.applyPose(working);

            MatrixCache matrices = new MatrixCache();
            instance.captureMatrices(matrices);
            MatrixCacheEntry entry = matrices.get(bone);
            if (entry != null && entry.matrix() != null)
            {
                /* [CRITICAL FIX: MODEL ARM MIRRORING + G-KEY JACOBIAN]
                 * Rotate 180° around Y to match rendered screen coordinates.
                 * Also dynamically updates bone matrix when transform.translate changes,
                 * ensuring GizmoDrag.computeTranslateJacobian produces a valid, invertible Jacobian. */
                return new Matrix4f(bodyPartBase)
                    .rotateY(mchorse.bbs_mod.utils.MathUtils.PI)
                    .mul(entry.matrix());
            }
        }
        finally
        {
            model.resetPose();
            model.applyPose(originalPose);
        }

        return bodyPartBase;
    }

    public void closeEditor()
    {
        if (active == this)
        {
            active = null;
        }

        if (this.filmPanel != null && this.filmPanel.getContext() != null)
        {
            this.filmPanel.getContext().closeContextMenu();
        }

        this.syncToBaseForm();

        this.removeFromParent();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null)
        {
            com.mojang.blaze3d.systems.RenderSystem.viewport(0, 0, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
        }

        if (this.dashboard != null)
        {
            this.dashboard.resize(this.dashboard.width, this.dashboard.height);
        }

        if (this.filmPanel instanceof UIFilmPanelPovAccess access && access.bbsPov$getEditor() != null)
        {
            access.bbsPov$getEditor().reloadHandModel();
        }
    }

    public void syncToBaseForm()
    {
        Form root = this.getRootForm();
        if (root != null && this.hand != null)
        {
            this.hand.baseForm.set(FormUtils.copy(root));
        }
    }

    public Replay getReplay()
    {
        return this.replay;
    }

    public Area getGapArea()
    {
        int screenW = this.area.w > 0 ? this.area.w : MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenH = this.area.h > 0 ? this.area.h : MinecraftClient.getInstance().getWindow().getScaledHeight();

        int left = this.formEditor != null && this.formEditor.forms != null && this.formEditor.forms.isVisible()
            ? this.formEditor.forms.area.ex()
            : this.area.x;

        int right = this.area.ex() > 0 ? this.area.ex() : screenW;

        if (this.formEditor != null && this.formEditor.editor != null)
        {
            if (this.formEditor.editor.view instanceof mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel panel
                && panel.options != null && panel.options.isVisible())
            {
                right = panel.options.area.x;
            }
            else if (this.formEditor.editor instanceof UIModelForm modelEditor
                && modelEditor.modelPanel != null && modelEditor.modelPanel.options != null
                && modelEditor.modelPanel.options.isVisible())
            {
                right = modelEditor.modelPanel.options.area.x;
            }
        }

        if (left <= 0)
        {
            left = (int) (screenW * 0.15F);
        }
        if (right >= screenW || right <= left)
        {
            right = (int) (screenW * 0.85F);
        }

        int gapX = left;
        int gapY = this.area.y;
        int gapW = Math.max(1, right - left);
        int gapH = Math.max(1, this.area.h > 0 ? this.area.h : screenH);

        return new Area(gapX, gapY, gapW, gapH);
    }

    public Area getFrameArea()
    {
        Area gap = this.getGapArea();

        float aspect = 16F / 9F;
        int frameW = gap.w;
        int frameH = Math.round(frameW / aspect);

        if (frameH > gap.h)
        {
            frameH = gap.h;
            frameW = Math.round(frameH * aspect);
        }

        int frameX = gap.x + (gap.w - frameW) / 2;
        int frameY = gap.y + (gap.h - frameH) / 2;

        return new Area(frameX, frameY, frameW, frameH);
    }

    @Override
    public void render(UIContext context)
    {
        this.configureEditor();

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF181818);

        Area gap = this.getGapArea();
        Area frame = this.getFrameArea();

        if (this.formEditor != null && this.formEditor.renderer != null)
        {
            this.formEditor.renderer.area.set(gap.x, gap.y, gap.w, gap.h);

            if (this.formEditor.renderer.getGizmoStencil() != null)
            {
                this.formEditor.renderer.getGizmoStencil().resizeGUI(gap.w, gap.h);
            }

            /* [PORTING NOTE: 16:9 LETTERBOX RENDERING FIX]
             * The 3D renderer area spans the full gap between left/right panels (gap).
             * To preserve the exact 70-degree FOV inside the 16:9 frame while allowing
             * the 3D model and bones to render outside into the top/bottom letterbox areas,
             * the vertical FOV is expanded proportionally by (gap.h / frame.h).
             * After super.render(), the transparent overlay (0x88000000) is drawn over
             * the top/bottom letterboxes, allowing bones outside 16:9 to remain visible
             * and transformable. */
            float halfTan = (float) Math.tan(Math.toRadians(35.0));
            float fovY = 2.0F * (float) Math.atan(halfTan * ((float) gap.h / (float) frame.h));
            this.formEditor.renderer.camera.fov = fovY;
        }

        boolean prevLight = mchorse.bbs_mod.BBSSettings.lightInputs;
        mchorse.bbs_mod.BBSSettings.lightInputs = true;
        try
        {
            super.render(context);
        }
        finally
        {
            mchorse.bbs_mod.BBSSettings.lightInputs = prevLight;
        }

        if (!this.isFormPickerOpen())
        {
            // Dark transparent overlay above the 16:9 frame
            if (frame.y > gap.y)
            {
                context.batcher.box(gap.x, gap.y, gap.ex(), frame.y, 0x88000000);
            }

            // Dark transparent overlay below the 16:9 frame
            if (frame.ey() < gap.ey())
            {
                context.batcher.box(gap.x, frame.ey(), gap.ex(), gap.ey(), 0x88000000);
            }

            // Left pillarbox (if any)
            if (frame.x > gap.x)
            {
                context.batcher.box(gap.x, frame.y, frame.x, frame.ey(), 0x88000000);
            }

            // Right pillarbox (if any)
            if (frame.ex() < gap.ex())
            {
                context.batcher.box(frame.ex(), frame.y, gap.ex(), frame.ey(), 0x88000000);
            }

            // 16:9 frame outline
            context.batcher.outline(frame.x, frame.y, frame.ex(), frame.ey(), 0x88ffffff);
        }
    }

    public boolean isFormPickerOpen()
    {
        if (this.formEditor != null)
        {
            for (UIFormList list : this.formEditor.getChildren(UIFormList.class))
            {
                if (list.isVisible())
                {
                    return true;
                }
            }
        }

        for (UIFormList list : this.getChildren(UIFormList.class))
        {
            if (list.isVisible())
            {
                return true;
            }
        }

        if (this.palette != null)
        {
            for (UIFormList list : this.palette.getChildren(UIFormList.class))
            {
                if (list.isVisible())
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.syncToBaseForm();
        return super.subMouseReleased(context);
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.closeEditor();
            return true;
        }

        return super.subKeyPressed(context);
    }
}
