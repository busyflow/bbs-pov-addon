package Glaxium.POV.actions.gui.render;

import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.gui.GuiRecipeBook;
import Glaxium.POV.actions.gui.GuiTypeEntry;
import Glaxium.POV.actions.gui.render.common.GuiTextRenderer;
import Glaxium.POV.hand.RecordedHandData;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.access.minecraft.HandledScreenPovAccess;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public final class GuiRenderContext {
   public final MatrixStack matrices;
   public final Batcher2D batcher;
   public final ReplayKeyframes replayKeyframes;
   public final GuiPovActionClip clip;
   public final RecordedHandData handData;
   public final RecordedHudData hudData;
   public final float globalTick;
   public final float localTick;
   public final int screenWidth;
   public final int screenHeight;
   public final boolean allowCursor;
   public final GuiTypeEntry entry;
   public final String guiId;
   public final Transform transform;
   public final float scaleX;
   public final float scaleY;
   public final float opacity;
   public final float bgOpacity;
   public final float originX;
   public final float originY;
   public final float unshiftedOriginX;
   public final boolean recipeOpen;
   public final boolean cursorVisible;
   public final boolean cursorHasItem;
   public final boolean isDragging;
   public final ItemStack cursorStack;
   public final String dragEncoded;
   public final float curScreenX;
   public final float curScreenY;
   public final float cursorGuiX;
   public final float cursorGuiY;
   public final GuiPointerHover hover = new GuiPointerHover();
   public boolean skipEntityPreview;

   private GuiRenderContext(
      MatrixStack matrices,
      Batcher2D batcher,
      ReplayKeyframes replayKeyframes,
      GuiPovActionClip clip,
      RecordedHandData handData,
      RecordedHudData hudData,
      float globalTick,
      float localTick,
      int screenWidth,
      int screenHeight,
      boolean allowCursor,
      GuiTypeEntry entry,
      String guiId,
      Transform transform,
      float scaleX,
      float scaleY,
      float opacity,
      float bgOpacity,
      float originX,
      float originY,
      float unshiftedOriginX,
      boolean recipeOpen,
      boolean cursorVisible,
      boolean cursorHasItem,
      boolean isDragging,
      ItemStack cursorStack,
      String dragEncoded,
      float curScreenX,
      float curScreenY,
      float cursorGuiX,
      float cursorGuiY
   ) {
      this.matrices = matrices;
      this.batcher = batcher;
      this.replayKeyframes = replayKeyframes;
      this.clip = clip;
      this.handData = handData;
      this.hudData = hudData;
      this.globalTick = globalTick;
      this.localTick = localTick;
      this.screenWidth = screenWidth;
      this.screenHeight = screenHeight;
      this.allowCursor = allowCursor;
      this.entry = entry;
      this.guiId = guiId;
      this.transform = transform;
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.opacity = opacity;
      this.bgOpacity = bgOpacity;
      this.originX = originX;
      this.originY = originY;
      this.unshiftedOriginX = unshiftedOriginX;
      this.recipeOpen = recipeOpen;
      this.cursorVisible = cursorVisible;
      this.cursorHasItem = cursorHasItem;
      this.isDragging = isDragging;
      this.cursorStack = cursorStack;
      this.dragEncoded = dragEncoded;
      this.curScreenX = curScreenX;
      this.curScreenY = curScreenY;
      this.cursorGuiX = cursorGuiX;
      this.cursorGuiY = cursorGuiY;
   }

   public static GuiRenderContext create(
      MatrixStack matrices,
      Batcher2D batcher,
      ReplayKeyframes replayKeyframes,
      GuiPovActionClip clip,
      RecordedHandData handData,
      float globalTick,
      int screenWidth,
      int screenHeight,
      boolean allowCursor
   ) {
      float localTick = clip.getLocalTick(globalTick);
      String stateId = clip.state.isEmpty() ? "inventory" : (String)clip.state.interpolate(localTick, "inventory");
      GuiTypeEntry entry = GuiTypeEntry.findById(stateId);
      if (entry == null) {
         entry = GuiTypeEntry.INVENTORY;
      }

      String guiId = entry.id;
      KeyframeChannel<Transform> layout = clip.getLayout(guiId);
      Transform transform = (layout != null && !layout.isEmpty() ? (Transform)layout.interpolate(localTick, new Transform()) : new Transform()).copy();
      float scaleX = Math.max(0.001F, transform.scale.x);
      float scaleY = Math.max(0.001F, transform.scale.y);
      KeyframeChannel<Float> opacityChannel = clip.getOpacity(guiId);
      float opacity = opacityChannel != null && !opacityChannel.isEmpty() ? (Float)opacityChannel.interpolate(localTick, 1.0F) : 1.0F;
      opacity = Math.max(0.0F, Math.min(1.0F, opacity));
      KeyframeChannel<Float> darkness = clip.getDarknessOpacity(guiId);
      float defaultDarkness = "gamemode_switcher".equals(guiId) ? 0.0F : 1.0F;
      float bgOpacity = darkness != null && !darkness.isEmpty() ? (Float)darkness.interpolate(localTick, defaultDarkness) : defaultDarkness;
      bgOpacity = Math.max(0.0F, Math.min(1.0F, bgOpacity));
      if (opacity <= 0.0F) {
         return null;
      } else {
         RecordedHudData hudData = replayKeyframes instanceof ReplayKeyframesPovAccess access ? access.bbsPov$getHud() : null;
         KeyframeChannel<Boolean> clipCursorVisible = clip.getCursorVisible(guiId);
         KeyframeChannel<Transform> clipCursorLayout = clip.getCursorLayout(guiId);
         KeyframeChannel<ItemStack> clipCursorItem = clip.getCursorItem(guiId);
         boolean useHudLayout = hudData != null && !hudData.cursorLayout.isEmpty();
         boolean useHudVisible = hudData != null && !hudData.cursorVisible.isEmpty();
         boolean useHudItem = hudData != null && !hudData.cursorItem.isEmpty();
         KeyframeChannel<Boolean> cursorVisibility = useHudVisible ? hudData.cursorVisible : clipCursorVisible;
         float visibleTick = useHudVisible ? globalTick : localTick;
         boolean cursorVisible = allowCursor
            && (
               cursorVisibility != null && !cursorVisibility.isEmpty()
                  ? (Boolean)cursorVisibility.interpolate(visibleTick, true)
                  : GuiTextRenderer.sampleBool(clipCursorVisible, localTick, true)
            );
         ItemStack cursorStack = ItemStack.EMPTY;
         boolean liveDragging = false;
         if (LiveGuiPreviewRenderer.isRenderingLive()
            && MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> handled
            && handled.getScreenHandler() != null) {
            ItemStack held = handled.getScreenHandler().getCursorStack();
            cursorStack = held == null ? ItemStack.EMPTY : held;
            if (handled instanceof HandledScreenPovAccess dragAccess) {
               liveDragging = dragAccess.bbsPov$isCursorDragging();
               if (liveDragging && dragAccess.bbsPov$getCursorDragSlots() != null && dragAccess.bbsPov$getCursorDragSlots().size() > 1) {
                  cursorStack = cursorStack.isEmpty() ? ItemStack.EMPTY : cursorStack.copyWithCount(Math.max(0, dragAccess.bbsPov$getDraggedStackRemainder()));
               }
            }
         }

         if (cursorStack == null || cursorStack.isEmpty()) {
            KeyframeChannel<ItemStack> cursorItemChannel = useHudItem ? hudData.cursorItem : clipCursorItem;
            float itemTick = useHudItem ? globalTick : localTick;
            if (cursorItemChannel != null && !cursorItemChannel.isEmpty()) {
               cursorStack = (ItemStack)cursorItemChannel.interpolate(itemTick, ItemStack.EMPTY);
            }
         }

         String dragEncoded = GuiTextRenderer.sampleString(clip.getDragSlots(guiId), localTick, "");
         boolean isDragging = dragEncoded != null && !dragEncoded.isEmpty() || liveDragging;
         boolean cursorHasItem = allowCursor && (cursorStack != null && !cursorStack.isEmpty() || isDragging);
         int guiW = entry.regionWidth;
         int guiH = entry.regionHeight;
         boolean recipeOpen = GuiRecipeBook.supports(guiId) && GuiTextRenderer.sampleBool(clip.getRecipeOpen(guiId), localTick, false);
         float originX = ((float)screenWidth - (float)guiW * scaleX) / 2.0F + transform.translate.x * 2.0F;
         float originY = screenHeight > guiH + 20
            ? (
               "book".equals(entry.id)
                  ? 2.0F * scaleY - transform.translate.y * 2.0F
                  : (
                     "gamemode_switcher".equals(entry.id)
                        ? (float)screenHeight / 2.0F - 58.0F * scaleY - transform.translate.y * 2.0F
                        : ((float)screenHeight - (float)guiH * scaleY) / 2.0F - transform.translate.y * 2.0F
                  )
            )
            : ((float)screenHeight - (float)guiH * scaleY) / 2.0F - transform.translate.y * 2.0F;
         float unshiftedOriginX = originX;
         if (recipeOpen) {
            originX += 77.0F * scaleX;
         }

         KeyframeChannel<Transform> cursorLayout = useHudLayout ? hudData.cursorLayout : clipCursorLayout;
         float layoutTick = useHudLayout ? globalTick : localTick;
         Transform cursorTransform = (cursorLayout != null && !cursorLayout.isEmpty()
               ? (Transform)cursorLayout.interpolate(layoutTick, new Transform())
               : new Transform())
            .copy();
         float curScreenX = (float)screenWidth / 2.0F + cursorTransform.translate.x * 2.0F;
         float curScreenY = (float)screenHeight / 2.0F - cursorTransform.translate.y * 2.0F;
         float cursorGuiX = (curScreenX - originX) / scaleX;
         float cursorGuiY = (curScreenY - originY) / scaleY;
         return new GuiRenderContext(
            matrices,
            batcher,
            replayKeyframes,
            clip,
            handData,
            hudData,
            globalTick,
            localTick,
            screenWidth,
            screenHeight,
            allowCursor,
            entry,
            guiId,
            transform,
            scaleX,
            scaleY,
            opacity,
            bgOpacity,
            originX,
            originY,
            unshiftedOriginX,
            recipeOpen,
            cursorVisible,
            cursorHasItem,
            isDragging,
            cursorStack,
            dragEncoded,
            curScreenX,
            curScreenY,
            cursorGuiX,
            cursorGuiY
         );
      }
   }
}
