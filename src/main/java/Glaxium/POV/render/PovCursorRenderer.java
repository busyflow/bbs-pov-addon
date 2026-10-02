package Glaxium.POV.render;

import Glaxium.POV.actions.gui.render.common.GuiSlotRenderer;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.hud.HudState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Draws the authored POV cursor overlay (item + pointer). */
public final class PovCursorRenderer
{
    private PovCursorRenderer()
    {
    }

    public static void render(Batcher2D batcher, HudState state, int width, int height)
    {
        if (state == null || !state.cursorVisible)
        {
            return;
        }

        float screenCenterX = width / 2F;
        float screenCenterY = height / 2F;
        float curScreenX = screenCenterX + (float) state.cursorLayout.translate.x * HotbarLayoutTransform.PIXELS_PER_UNIT;
        float curScreenY = screenCenterY - (float) state.cursorLayout.translate.y * HotbarLayoutTransform.PIXELS_PER_UNIT;

        float scaleX = Math.max(0.001F, state.cursorLayout.scale.x);
        float scaleY = Math.max(0.001F, state.cursorLayout.scale.y);

        ItemStack cursorStack = state.cursorItem;
        if (cursorStack != null && !cursorStack.isEmpty())
        {
            /* DrawContext.drawItem() temporarily changes the global GUI
             * model-view stack. Survival Inventory renders its player preview
             * through that same stack on the following GUI pass; without this
             * guard the carried block leaves the actor projected in world space.
             * Creative has no equivalent player-preview pass, hence it hid the
             * leak. */
            MatrixStack guiMatrices = batcher.getContext().getMatrices();
            MatrixStack modelView = RenderSystem.getModelViewStack();
            Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
            guiMatrices.push();
            modelView.push();

            try
            {
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.depthMask(true);
                RenderSystem.clearDepth(1D);
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
                DiffuseLighting.enableGuiDepthLighting();
                GuiSlotRenderer.drawSlotItem(batcher, cursorStack, (int) (curScreenX - 8), (int) (curScreenY - 8));
                batcher.flush();
            }
            finally
            {
                DiffuseLighting.disableGuiDepthLighting();
                modelView.pop();
                RenderSystem.applyModelViewMatrix();
                guiMatrices.pop();
                RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_Z);
                /* ItemRenderer may leave a non-standard depth comparison or
                 * colour mask behind. Survival's entity preview is the next
                 * 3D GUI draw, so reset the complete contract it relies on. */
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.clearDepth(1D);
                /* ItemRenderer leaves culling disabled for some generated/block
                 * models.  BBS's player preview can still submit its translucent
                 * faces after this call; with culling off, rear faces at equal
                 * depth overwrite the front faces and the preview looks torn
                 * apart. */
                RenderSystem.enableCull();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
            }
        }

        batcher.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MatrixStack matrices = batcher.getContext().getMatrices();
        matrices.push();
        matrices.translate(curScreenX, curScreenY, 0F);
        if (state.cursorLayout.rotate.z != 0F)
        {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(state.cursorLayout.rotate.z));
        }
        float defaultScale = PovSettings.getCursorDefaultScale();
        matrices.scale(scaleX * defaultScale, scaleY * defaultScale, 1F);
        PovSettings.renderCursor(batcher);
        batcher.flush();
        matrices.pop();
    }
}
