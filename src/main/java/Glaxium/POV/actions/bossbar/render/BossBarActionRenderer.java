package Glaxium.POV.actions.bossbar.render;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.RecordedPovActions;
import Glaxium.POV.actions.bossbar.BossBarLooks;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.integration.access.minecraft.BossBarHudPovAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class BossBarActionRenderer {
   private static final int WIDTH = 182;
   private static final int HEIGHT = 5;
   private static final int BAR_Y = 12;
   private static final Identifier BARS_TEXTURE = new Identifier("textures/gui/bars.png");

   private BossBarActionRenderer() {
   }

   public static void render(Batcher2D batcher, RecordedPovActions actions, float tick, int width, int height) {
      if (actions != null) {
         List<BossBarPovActionClip> activeClips = actions.getActiveBossBars(tick);
         if (!activeClips.isEmpty()) {
            int baseLayer = PovActionType.BOSS_BARS.seedLayer();
            int maxSlots = 9;

            for (int i = 0; i < Math.min(maxSlots, activeClips.size()); i++) {
               BossBarPovActionClip clip = activeClips.get(i);
               float local = clip.getLocalTick(tick);
               int slot = Math.max(0, Math.min(8, (Integer)clip.layer.get() - baseLayer));
               if (slot < 0 || slot >= maxSlots) {
                  slot = i;
               }

               int y = 12 + slot * 19;
               renderBar(
                  batcher,
                  (String)clip.name.interpolate(local, "Ender Dragon"),
                  (Float)clip.percent.interpolate(local, 1.0F),
                  (String)clip.color.interpolate(local, "pink"),
                  (String)clip.style.interpolate(local, "progress"),
                  width,
                  y
               );
            }
         }
      }
   }

   public static void renderLive(Batcher2D batcher, int width, int height) {
      MinecraftClient client = MinecraftClient.getInstance();
      if ((client.inGameHud == null ? null : client.inGameHud.getBossBarHud()) instanceof BossBarHudPovAccess access) {
         Map<UUID, ClientBossBar> bars = access.bbsPov$getBossBars();
         if (bars != null && !bars.isEmpty()) {
            int slot = 0;

            for (ClientBossBar bar : bars.values()) {
               if (bar != null) {
                  int y = 12 + slot * 19;
                  renderBar(
                     batcher, bar.getName().getString(), bar.getPercent(), BossBarLooks.colorId(bar.getColor()), BossBarLooks.styleId(bar.getStyle()), width, y
                  );
                  if (++slot >= 9) {
                     break;
                  }
               }
            }
         }
      }
   }

   private static void renderBar(Batcher2D batcher, String name, float percent, String color, String style, int screenWidth, int y) {
      int x = screenWidth / 2 - 91;
      int fill = Math.round(MathHelper.clamp(percent, 0.0F, 1.0F) * 182.0F);
      DrawContext context = batcher.getContext();
      batcher.flush();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int colorIdx = BossBarLooks.colorIndex(color);
      int vBg = colorIdx * 10;
      int vProg = colorIdx * 10 + 5;
      context.drawTexture(BARS_TEXTURE, x, y, 0, vBg, 182, 5);
      if (fill > 0) {
         context.drawTexture(BARS_TEXTURE, x, y, 0, vProg, fill, 5);
      }

      int styleIdx = BossBarLooks.styleIndex(style);
      if (styleIdx > 0) {
         int vNotchBg = 80 + (styleIdx - 1) * 10;
         int vNotchProg = 80 + (styleIdx - 1) * 10 + 5;
         context.drawTexture(BARS_TEXTURE, x, y, 0, vNotchBg, 182, 5);
         if (fill > 0) {
            context.drawTexture(BARS_TEXTURE, x, y, 0, vNotchProg, fill, 5);
         }
      }

      TextRenderer texts = MinecraftClient.getInstance().textRenderer;
      Text title = Text.literal(name == null ? "" : name);
      int textX = screenWidth / 2 - texts.getWidth(title) / 2;
      context.drawTextWithShadow(texts, title, textX, y - 9, 16777215);
      context.draw();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      batcher.flush();
   }
}
