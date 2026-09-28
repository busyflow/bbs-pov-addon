package Glaxium.POV.render;

import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.PovCameraClips;
import Glaxium.POV.playback.PovPlaybackContext;
import Glaxium.POV.replay.PovReplaySettings;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class PovBlockOutlineRenderer {
   private static final double FIRST_PERSON_REACH = 4.5;
   private static final double THIRD_PERSON_REACH = 8.5;

   private PovBlockOutlineRenderer() {
   }

   public static void init() {
      WorldRenderEvents.AFTER_ENTITIES.register(PovBlockOutlineRenderer::render);
   }

   private static void render(WorldRenderContext context) {
      if (shouldRenderOutline(context.tickDelta())) {
         Camera camera = context.camera();
         ClientWorld world = context.world();
         MinecraftClient client = MinecraftClient.getInstance();
         if (camera != null && world != null && client != null) {
            Vec3d cameraPos = camera.getPos();
            Vec3d dir = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            double reach = 5.0;
            Entity cameraEntity = (Entity)(client.cameraEntity != null ? client.cameraEntity : client.player);
            BlockHitResult hit = world.raycast(
               new RaycastContext(cameraPos, cameraPos.add(dir.multiply(reach)), ShapeType.OUTLINE, FluidHandling.NONE, cameraEntity)
            );
            if (hit != null && hit.getType() == Type.BLOCK) {
               BlockPos blockPos = hit.getBlockPos();
               BlockState state = world.getBlockState(blockPos);
               if (!state.isAir() && world.getWorldBorder().contains(blockPos)) {
                  MatrixStack matrices = context.matrixStack();
                  VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getLines());
                  VoxelShape shape = state.getOutlineShape(world, blockPos, ShapeContext.of(cameraEntity));
                  drawUnifiedShapeOutline(
                     matrices,
                     consumer,
                     shape,
                     (double)blockPos.getX() - cameraPos.x,
                     (double)blockPos.getY() - cameraPos.y,
                     (double)blockPos.getZ() - cameraPos.z,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.4F
                  );
               }
            }
         }
      }
   }

   private static void drawUnifiedShapeOutline(
      MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape shape, double offsetX, double offsetY, double offsetZ, float r, float g, float b, float a
   ) {
      Entry entry = matrices.peek();
      shape.forEachEdge(
         (x1, y1, z1, x2, y2, z2) -> {
            float dx = (float)(x2 - x1);
            float dy = (float)(y2 - y1);
            float dz = (float)(z2 - z1);
            float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= len;
            dy /= len;
            dz /= len;
            vertexConsumer.vertex(entry.getPositionMatrix(), (float)(x1 + offsetX), (float)(y1 + offsetY), (float)(z1 + offsetZ))
               .color(r, g, b, a)
               .normal(entry.getNormalMatrix(), dx, dy, dz)
               .next();
            vertexConsumer.vertex(entry.getPositionMatrix(), (float)(x2 + offsetX), (float)(y2 + offsetY), (float)(z2 + offsetZ))
               .color(r, g, b, a)
               .normal(entry.getNormalMatrix(), dx, dy, dz)
               .next();
         }
      );
   }

   private static boolean shouldRenderOutline(float tickDelta) {
      UIFilmPanel panel = PovReplaySettings.getFilmPanel();
      if (panel != null && panel.getController() != null) {
         int povMode = panel.getController().getPovMode();
         if (povMode == 6) {
            return true;
         }

         Film film = (Film)panel.getData();
         float filmTick = panel.getRunner() != null && panel.getRunner().isRunning() ? (float)panel.getRunner().ticks + tickDelta : (float)panel.getCursor();
         PovCameraClip clip = PovCameraClips.resolve(film, filmTick);
         if (clip != null && (Boolean)clip.headLook.get() && (Boolean)clip.blockOutline.get()) {
            return true;
         }
      }

      PovPlaybackContext.Frame frame = PovPlaybackContext.getActive(tickDelta);
      if (frame != null) {
         return frame.clip() == null ? true : (Boolean)frame.clip().headLook.get() && (Boolean)frame.clip().blockOutline.get();
      } else {
         return false;
      }
   }
}
