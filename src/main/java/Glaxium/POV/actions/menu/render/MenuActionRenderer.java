package Glaxium.POV.actions.menu.render;

import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.menu.schema.MenuTypeResolver;
import Glaxium.POV.integration.access.minecraft.DeathScreenPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.GridWidget.Adder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MenuActionRenderer {
   private static final int DEATH_TOP = 1615855616;
   private static final int DEATH_BOTTOM = -1602211792;
   private static final int PAUSE_TOP = -1072689136;
   private static final int PAUSE_BOTTOM = -804253680;
   private static final MenuPovActionClip LIVE_CLIP = new MenuPovActionClip();
   private static final Transform LIVE_CURSOR = new Transform();
   private static GridWidget pauseGrid;
   private static int pauseWidth;
   private static int pauseHeight;

   private MenuActionRenderer() {
   }

   public static void render(MatrixStack matrices, Batcher2D batcher, RecordedPovActions actions, float tick, int screenWidth, int screenHeight) {
      if (actions != null) {
         MenuPovActionClip clip = actions.getActiveMenu(tick);
         if (clip != null) {
            renderClip(batcher, clip, clip.getLocalTick(tick), screenWidth, screenHeight);
         }
      }
   }

   public static boolean renderLive(Batcher2D batcher, int screenWidth, int screenHeight) {
      MinecraftClient client = MinecraftClient.getInstance();
      String type = MenuTypeResolver.resolveLive(client.currentScreen, client.player);
      if (type == null) {
         return false;
      } else {
         fillLiveClip(type, client.currentScreen, client.player, screenWidth, screenHeight);
         renderClip(batcher, LIVE_CLIP, 0.0F, screenWidth, screenHeight);
         return true;
      }
   }

   public static void renderClip(Batcher2D batcher, MenuPovActionClip clip, float local, int screenWidth, int screenHeight) {
      String type = clip.state.isEmpty() ? "game_menu" : (String)clip.state.interpolate(local, "game_menu");
      if (type == null || type.isBlank()) {
         type = "game_menu";
      }

      float mouseX = (float)screenWidth / 2.0F;
      float mouseY = (float)screenHeight / 2.0F;
      boolean cursorVisible = clip.cursorVisible.isEmpty() || Boolean.TRUE.equals(clip.cursorVisible.interpolate(local, true));
      if (cursorVisible && !clip.cursorLayout.isEmpty()) {
         Transform cursor = (Transform)clip.cursorLayout.interpolate(local, new Transform());
         mouseX = (float)screenWidth / 2.0F + cursor.translate.x * 2.0F;
         mouseY = (float)screenHeight / 2.0F - cursor.translate.y * 2.0F;
      }

      DrawContext context = batcher.getContext();
      batcher.flush();
      RenderSystem.depthMask(true);
      RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
      overlayState();
      switch (type) {
         case "death":
            renderDeath(context, batcher, clip, local, screenWidth, screenHeight, mouseX, mouseY);
            break;
         case "sleep":
            renderSleep(context, batcher, clip, local, screenWidth, screenHeight, mouseX, mouseY);
            break;
         default:
            renderGameMenu(context, batcher, screenWidth, screenHeight, mouseX, mouseY);
      }

      overlayState();
      batcher.flush();
   }

   private static void fillLiveClip(String type, Screen screen, ClientPlayerEntity player, int width, int height) {
      LIVE_CLIP.tick.set(0);
      LIVE_CLIP.duration.set(100);
      setChannel(LIVE_CLIP.state, type);
      MinecraftClient client = MinecraftClient.getInstance();
      double mouseX = client.mouse.getX() * (double)client.getWindow().getScaledWidth() / (double)client.getWindow().getWidth();
      double mouseY = client.mouse.getY() * (double)client.getWindow().getScaledHeight() / (double)client.getWindow().getHeight();
      LIVE_CURSOR.translate.set((float)((mouseX - (double)width / 2.0) / 2.0), (float)(((double)height / 2.0 - mouseY) / 2.0), 0.0F);
      setChannel(LIVE_CLIP.cursorLayout, LIVE_CURSOR);
      boolean leaveBed = screen instanceof SleepingChatScreen;
      setChannel(LIVE_CLIP.leaveBed, leaveBed);
      setChannel(LIVE_CLIP.cursorVisible, !"sleep".equals(type) || leaveBed);
      if ("death".equals(type) && screen instanceof DeathScreen) {
         String message = "";
         String score = "Score: 0";
         boolean active = false;
         if (screen instanceof DeathScreenPovAccess deathAccess) {
            Text deathMessage = deathAccess.bbsPov$getDeathMessage();
            Text scoreText = deathAccess.bbsPov$getScoreText();
            if (deathMessage != null) {
               message = deathMessage.getString();
            }

            if (scoreText != null) {
               score = scoreText.getString();
            }

            active = deathAccess.bbsPov$getTicksSinceDeath() >= 20;
         }

         setChannel(LIVE_CLIP.deathMessage, message);
         setChannel(LIVE_CLIP.score, score);
         setChannel(LIVE_CLIP.bgOpacity, 1.0F);
         setChannel(LIVE_CLIP.buttonsActive, active);
      } else if ("sleep".equals(type)) {
         int timer = player == null ? 0 : player.getSleepTimer();
         setChannel(LIVE_CLIP.opacity, MenuSleepOverlay.progress(timer));
      }
   }

   private static <T> void setChannel(KeyframeChannel<T> channel, T value) {
      if (channel != null) {
         if (channel.isEmpty()) {
            channel.insert(0.0F, value);
         } else {
            ((Keyframe)channel.getKeyframes().get(0)).setValue(value);
         }
      }
   }

   private static void renderGameMenu(DrawContext context, Batcher2D batcher, int width, int height, float mouseX, float mouseY) {
      context.fillGradient(0, 0, width, height, -1072689136, -804253680);
      overlayState();
      TextRenderer texts = MinecraftClient.getInstance().textRenderer;
      context.drawCenteredTextWithShadow(texts, Text.translatable("menu.game"), width / 2, 40, 16777215);
      GridWidget grid = pauseButtons(width, height);
      grid.forEachChild(child -> {
         overlayState();
         child.render(context, (int)mouseX, (int)mouseY, 0.0F);
      });
      overlayState();
      batcher.flush();
   }

   private static GridWidget pauseButtons(int width, int height) {
      if (pauseGrid != null && pauseWidth == width && pauseHeight == height) {
         return pauseGrid;
      } else {
         GridWidget grid = new GridWidget();
         grid.getMainPositioner().margin(4, 4, 4, 0);
         Adder adder = grid.createAdder(2);
         adder.add(ButtonWidget.builder(Text.translatable("menu.returnToGame"), b -> {
         }).width(204).build(), 2, grid.copyPositioner().marginTop(50));
         adder.add(narrowButton("gui.advancements"));
         adder.add(narrowButton("gui.stats"));
         adder.add(narrowButton("menu.sendFeedback"));
         adder.add(narrowButton("menu.reportBugs"));
         adder.add(narrowButton("menu.options"));
         adder.add(narrowButton("menu.shareToLan"));
         adder.add(ButtonWidget.builder(Text.translatable("menu.returnToMenu"), b -> {
         }).width(204).build(), 2);
         grid.refreshPositions();
         SimplePositioningWidget.setPos(grid, 0, 0, width, height, 0.5F, 0.25F);
         pauseGrid = grid;
         pauseWidth = width;
         pauseHeight = height;
         return grid;
      }
   }

   private static ButtonWidget narrowButton(String key) {
      return ButtonWidget.builder(Text.translatable(key), b -> {
      }).width(98).build();
   }

   private static void renderDeath(
      DrawContext context, Batcher2D batcher, MenuPovActionClip clip, float local, int width, int height, float mouseX, float mouseY
   ) {
      float bg = clip.bgOpacity.isEmpty() ? 1.0F : (Float)clip.bgOpacity.interpolate(local, 1.0F);
      bg = Math.max(0.0F, Math.min(1.0F, bg));
      context.fillGradient(0, 0, width, height, scaleAlpha(1615855616, bg), scaleAlpha(-1602211792, bg));
      overlayState();
      boolean buttonsActive = clip.buttonsActive.isEmpty() ? local >= 20.0F : Boolean.TRUE.equals(clip.buttonsActive.interpolate(local, false));
      int buttonX = width / 2 - 100;
      drawButton(context, mouseX, mouseY, buttonX, height / 4 + 72, 200, 20, Text.translatable("deathScreen.respawn"), buttonsActive);
      drawButton(context, mouseX, mouseY, buttonX, height / 4 + 96, 200, 20, Text.translatable("deathScreen.titleScreen"), buttonsActive);
      TextRenderer texts = MinecraftClient.getInstance().textRenderer;
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.scale(2.0F, 2.0F, 2.0F);
      context.drawCenteredTextWithShadow(texts, Text.translatable("deathScreen.title"), width / 2 / 2, 30, 16777215);
      matrices.pop();
      String message = clip.deathMessage.isEmpty() ? "" : (String)clip.deathMessage.interpolate(local, "");
      if (message != null && !message.isBlank()) {
         context.drawCenteredTextWithShadow(texts, Text.literal(message), width / 2, 85, 16777215);
      }

      String score = clip.score.isEmpty() ? "Score: 0" : (String)clip.score.interpolate(local, "Score: 0");
      if (score.regionMatches(true, 0, "Score:", 0, 6)) {
         String value = score.substring(6).trim();
         int labelWidth = texts.getWidth("Score: ");
         int valueWidth = texts.getWidth(value);
         int x = width / 2 - (labelWidth + valueWidth) / 2;
         context.drawTextWithShadow(texts, Text.literal("Score: "), x, 100, 16777215);
         context.drawTextWithShadow(texts, Text.literal(value).formatted(Formatting.YELLOW), x + labelWidth, 100, 16777045);
      } else {
         context.drawCenteredTextWithShadow(texts, Text.literal(score), width / 2, 100, 16777215);
      }

      overlayState();
      batcher.flush();
   }

   private static void renderSleep(
      DrawContext context, Batcher2D batcher, MenuPovActionClip clip, float local, int width, int height, float mouseX, float mouseY
   ) {
      float progress = clip.opacity.isEmpty() ? 1.0F : (Float)clip.opacity.interpolate(local, 1.0F);
      int color = MenuSleepOverlay.color(progress);
      if (color >>> 24 > 0) {
         context.fill(RenderLayer.getGuiOverlay(), 0, 0, width, height, color);
      }

      boolean showLeaveBed = clip.leaveBed.isEmpty() || Boolean.TRUE.equals(clip.leaveBed.interpolate(local, true));
      if (showLeaveBed) {
         drawButton(context, mouseX, mouseY, width / 2 - 100, height - 40, 200, 20, Text.translatable("multiplayer.stopSleeping"), true);
      }

      overlayState();
      batcher.flush();
   }

   private static void drawButton(DrawContext context, float mouseX, float mouseY, int x, int y, int w, int h, Text label, boolean active) {
      ButtonWidget button = ButtonWidget.builder(label, b -> {
      }).dimensions(x, y, w, h).build();
      button.active = active;
      overlayState();
      button.render(context, (int)mouseX, (int)mouseY, 0.0F);
      overlayState();
   }

   private static int scaleAlpha(int argb, float factor) {
      int alpha = Math.round((float)(argb >>> 24 & 0xFF) * factor);
      return alpha << 24 | argb & 16777215;
   }

   private static void overlayState() {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static boolean hidesCursor(RecordedPovActions actions, float tick) {
      if (actions == null) {
         return false;
      } else {
         MenuPovActionClip clip = actions.getActiveMenu(tick);
         if (clip == null) {
            return false;
         } else {
            float local = clip.getLocalTick(tick);
            String type = clip.state.isEmpty() ? "game_menu" : (String)clip.state.interpolate(local, "game_menu");
            if ("sleep".equals(type)) {
               boolean showLeaveBed = clip.leaveBed.isEmpty() || Boolean.TRUE.equals(clip.leaveBed.interpolate(local, true));
               if (!showLeaveBed) {
                  return true;
               }
            }

            return !clip.cursorVisible.isEmpty() && !Boolean.TRUE.equals(clip.cursorVisible.interpolate(local, true));
         }
      }
   }

   public static boolean blocksCrosshair(RecordedPovActions actions, float tick) {
      if (actions == null) {
         return false;
      } else {
         MenuPovActionClip clip = actions.getActiveMenu(tick);
         if (clip == null) {
            return false;
         } else {
            float local = clip.getLocalTick(tick);
            String type = clip.state.isEmpty() ? "game_menu" : (String)clip.state.interpolate(local, "game_menu");
            return !"sleep".equals(type) || !hidesCursor(actions, tick);
         }
      }
   }
}
