package Glaxium.POV.render;

import Glaxium.POV.camera.PovCameraMode;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FirstPersonFilmController;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

/**
 * Renders vanilla block selection outline when in POV camera mode or in a POV
 * camera clip with Head Look and Block Outline enabled.
 */
public final class PovBlockOutlineRenderer
{
    private static final double BLOCK_REACH = 4.5D;

    private PovBlockOutlineRenderer()
    {
    }

    public static void init()
    {
        WorldRenderEvents.AFTER_ENTITIES.register(PovBlockOutlineRenderer::render);
    }

    private static void render(WorldRenderContext context)
    {
        if (!shouldRenderOutline(context.tickDelta()))
        {
            return;
        }

        Camera camera = context.camera();
        ClientWorld world = context.world();
        MinecraftClient client = MinecraftClient.getInstance();

        if (camera == null || world == null || client == null)
        {
            return;
        }

        Vec3d cameraPos = camera.getPos();
        Vec3d dir = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        double reach = BLOCK_REACH;

        var cameraEntity = client.cameraEntity != null ? client.cameraEntity : client.player;
        BlockHitResult hit = world.raycast(new RaycastContext(
            cameraPos,
            cameraPos.add(dir.multiply(reach)),
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            cameraEntity
        ));

        if (hit != null && hit.getType() == HitResult.Type.BLOCK)
        {
            BlockPos blockPos = hit.getBlockPos();
            BlockState state = world.getBlockState(blockPos);

            if (!state.isAir() && world.getWorldBorder().contains(blockPos))
            {
                MatrixStack matrices = context.matrixStack();
                VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
                VoxelShape shape = state.getOutlineShape(world, blockPos, ShapeContext.of(cameraEntity));

                drawUnifiedShapeOutline(
                    matrices,
                    consumer,
                    shape,
                    (double) blockPos.getX() - cameraPos.x,
                    (double) blockPos.getY() - cameraPos.y,
                    (double) blockPos.getZ() - cameraPos.z,
                    0.0F, 0.0F, 0.0F, 0.4F
                );
            }
        }
    }

    private static void drawUnifiedShapeOutline(
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        VoxelShape shape,
        double offsetX,
        double offsetY,
        double offsetZ,
        float r,
        float g,
        float b,
        float a)
    {
        MatrixStack.Entry entry = matrices.peek();
        shape.forEachEdge((x1, y1, z1, x2, y2, z2) ->
        {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float len = net.minecraft.util.math.MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            vertexConsumer.vertex(entry.getPositionMatrix(), (float) (x1 + offsetX), (float) (y1 + offsetY), (float) (z1 + offsetZ))
                .color(r, g, b, a)
                .normal(entry.getNormalMatrix(), dx, dy, dz)
                .next();
            vertexConsumer.vertex(entry.getPositionMatrix(), (float) (x2 + offsetX), (float) (y2 + offsetY), (float) (z2 + offsetZ))
                .color(r, g, b, a)
                .normal(entry.getNormalMatrix(), dx, dy, dz)
                .next();
        });
    }

    private static boolean shouldRenderOutline(float tickDelta)
    {
        /* Case 1: Film Editor is open */
        UIFilmPanel panel = PovReplaySettings.getFilmPanel();
        if (panel != null && panel.getController() != null)
        {
            int povMode = panel.getController().getPovMode();
            if (povMode == PovCameraMode.POV)
            {
                return true;
            }

            Film film = (Film) panel.getData();
            float filmTick = panel.getRunner() != null && panel.getRunner().isRunning()
                ? panel.getRunner().ticks + tickDelta
                : panel.getCursor();

            PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
            if (clip != null && clip.headLook.get() && clip.blockOutline.get())
            {
                return true;
            }
        }

        /* Case 2: In-world playback */
        PovPlaybackContext.Frame frame = PovPlaybackContext.getActive(tickDelta);
        if (frame != null)
        {
            if (frame.clip() != null)
            {
                return frame.clip().headLook.get() && frame.clip().blockOutline.get();
            }
            return true;
        }

        return false;
    }
}
