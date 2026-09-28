package Glaxium.POV.hand.render;

import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.IntBuffer;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class PovHandDepthManager {
   private static Framebuffer worldDepthFbo;
   private static Framebuffer replayDepthFbo;
   private static Framebuffer replayOnlyDepthFbo;
   private static Framebuffer savedSceneDepthFbo;
   private static Framebuffer tempHandDepthFbo;
   private static boolean hasCapturedWorldDepth = false;
   private static boolean hasCapturedReplayDepth = false;
   private static boolean hasSavedSceneDepth = false;
   private static int replayOnlyProgram = 0;
   private static int uReplayWorldDepthLoc = 0;
   private static int uReplayFullDepthLoc = 0;
   private static int compressProgram = 0;
   private static int uCompressSavedDepthLoc = 0;
   private static int uCompressWorldDepthLoc = 0;
   private static int uCompressModeLoc = 0;
   private static int mergeProgram = 0;
   private static int uMergeSavedSceneDepthLoc = 0;
   private static int uMergeHandDepthLoc = 0;
   private static int uMergeWorldDepthLoc = 0;
   private static int uMergeModeLoc = 0;
   private static int currentInteractionIrisMode = 0;
   private static int quadVao = 0;
   private static int quadVbo = 0;
   private static final IntBuffer viewportBuf = BufferUtils.createIntBuffer(16);

   private PovHandDepthManager() {
   }

   public static void onRenderWorldStart() {
      hasCapturedWorldDepth = false;
      hasCapturedReplayDepth = false;
      hasSavedSceneDepth = false;
   }

   public static void onBeforeReplayRender() {
      if (!hasCapturedWorldDepth) {
         captureWorldDepth();
         hasCapturedWorldDepth = true;
      }
   }

   public static void onRenderWorldEnd() {
      if (!hasCapturedWorldDepth) {
         captureWorldDepth();
         hasCapturedWorldDepth = true;
      }
   }

   private static int getRenderWidth() {
      int videoWidth = BBSRendering.getVideoWidth();
      if (videoWidth > 0) {
         return videoWidth;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         return client.getFramebuffer() != null && client.getFramebuffer().textureWidth > 0
            ? client.getFramebuffer().textureWidth
            : client.getWindow().getFramebufferWidth();
      }
   }

   private static int getRenderHeight() {
      int videoHeight = BBSRendering.getVideoHeight();
      if (videoHeight > 0) {
         return videoHeight;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         return client.getFramebuffer() != null && client.getFramebuffer().textureHeight > 0
            ? client.getFramebuffer().textureHeight
            : client.getWindow().getFramebufferHeight();
      }
   }

   private static Framebuffer ensureFbo(Framebuffer current, int width, int height) {
      if (current == null) {
         current = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
         current.setTexFilter(9728);
      } else if (current.textureWidth != width || current.textureHeight != height) {
         current.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
         current.setTexFilter(9728);
      }

      return current;
   }

   public static void captureWorldDepth() {
      int width = getRenderWidth();
      int height = getRenderHeight();
      if (width > 0 && height > 0) {
         worldDepthFbo = ensureFbo(worldDepthFbo, width, height);
         blitDepthFromCurrent(worldDepthFbo);
      }
   }

   public static void captureReplayDepth() {
      int width = getRenderWidth();
      int height = getRenderHeight();
      if (width > 0 && height > 0) {
         replayDepthFbo = ensureFbo(replayDepthFbo, width, height);
         blitDepthFromCurrent(replayDepthFbo);
         hasCapturedReplayDepth = true;
      }
   }

   public static void prepareDepthForHand(boolean worldInteraction, boolean replayInteraction) {
      beginHandDepth(worldInteraction, replayInteraction);
   }

   public static void beginHandDepth(boolean worldInteraction, boolean replayInteraction) {
      if (!hasCapturedWorldDepth) {
         captureWorldDepth();
         hasCapturedWorldDepth = true;
      }

      int width = getRenderWidth();
      int height = getRenderHeight();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.depthFunc(515);
      if (BBSRendering.isIrisShadersEnabled()) {
         savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
         blitDepthFromCurrent(savedSceneDepthFbo);
         hasSavedSceneDepth = true;
         int mode;
         if (worldInteraction && replayInteraction) {
            mode = 0;
         } else if (worldInteraction && !replayInteraction) {
            mode = 1;
         } else if (!worldInteraction && replayInteraction) {
            mode = 2;
         } else {
            mode = 3;
         }

         currentInteractionIrisMode = mode;
         compressDepthToCurrent(width, height, mode);
      } else {
         if (!worldInteraction && !replayInteraction) {
            hasSavedSceneDepth = false;
            RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
         } else if (worldInteraction && replayInteraction) {
            hasSavedSceneDepth = false;
            if (hasCapturedReplayDepth && replayDepthFbo != null) {
               blitDepthToCurrent(replayDepthFbo);
            } else if (hasCapturedWorldDepth && worldDepthFbo != null) {
               blitDepthToCurrent(worldDepthFbo);
            }
         } else if (worldInteraction && !replayInteraction) {
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;
            if (hasCapturedWorldDepth && worldDepthFbo != null) {
               blitDepthToCurrent(worldDepthFbo);
            } else {
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            }
         } else if (!worldInteraction && replayInteraction) {
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;
            if (hasCapturedWorldDepth && worldDepthFbo != null && hasCapturedReplayDepth && replayDepthFbo != null) {
               updateReplayOnlyDepth(width, height);
               blitDepthToCurrent(replayOnlyDepthFbo);
            } else if (hasCapturedReplayDepth && replayDepthFbo != null) {
               blitDepthToCurrent(replayDepthFbo);
            } else {
               RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            }
         }
      }
   }

   public static void endHandDepth(boolean worldInteraction, boolean replayInteraction) {
      if (hasSavedSceneDepth && savedSceneDepthFbo != null) {
         hasSavedSceneDepth = false;
         int width = getRenderWidth();
         int height = getRenderHeight();
         if (width > 0 && height > 0) {
            if (BBSRendering.isIrisShadersEnabled()) {
               mergeDepthWithSaved(width, height, currentInteractionIrisMode);
            } else {
               mergeDepthWithSaved(width, height, -1);
            }
         }
      }
   }

   private static void blitDepthFromCurrent(Framebuffer dst) {
      int prevRead = GL30.glGetInteger(36010);
      int prevDraw = GL30.glGetInteger(36006);
      int srcFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
      if (srcFbo == 0) {
         Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
         if (main != null) {
            srcFbo = main.fbo;
         }
      }

      if (srcFbo != 0 && srcFbo != dst.fbo) {
         GL30.glBindFramebuffer(36008, srcFbo);
         GL30.glBindFramebuffer(36009, dst.fbo);
         GL30.glBlitFramebuffer(0, 0, dst.textureWidth, dst.textureHeight, 0, 0, dst.textureWidth, dst.textureHeight, 256, 9728);
         GL30.glBindFramebuffer(36008, prevRead);
         GL30.glBindFramebuffer(36009, prevDraw);
      }
   }

   private static void blitDepthToCurrent(Framebuffer src) {
      if (src != null) {
         int prevRead = GL30.glGetInteger(36010);
         int prevDraw = GL30.glGetInteger(36006);
         int dstFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
         if (dstFbo == 0) {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null) {
               dstFbo = main.fbo;
            }
         }

         if (dstFbo != 0 && dstFbo != src.fbo) {
            GL30.glBindFramebuffer(36008, src.fbo);
            GL30.glBindFramebuffer(36009, dstFbo);
            GL30.glBlitFramebuffer(0, 0, src.textureWidth, src.textureHeight, 0, 0, src.textureWidth, src.textureHeight, 256, 9728);
            GL30.glBindFramebuffer(36008, prevRead);
            GL30.glBindFramebuffer(36009, prevDraw);
         }
      }
   }

   private static void compressDepthToCurrent(int width, int height, int mode) {
      initCompressShader();
      if (compressProgram != 0) {
         int prevRead = GL30.glGetInteger(36010);
         int prevDraw = GL30.glGetInteger(36006);
         int targetFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
         if (targetFbo == 0) {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null) {
               targetFbo = main.fbo;
            }
         }

         if (targetFbo != 0) {
            viewportBuf.clear();
            GL11.glGetIntegerv(2978, viewportBuf);
            GL30.glBindFramebuffer(36160, targetFbo);
            GL11.glViewport(0, 0, width, height);
            GL20.glUseProgram(compressProgram);
            GL13.glActiveTexture(33984);
            int prevTex0 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, savedSceneDepthFbo.getDepthAttachment());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL20.glUniform1i(uCompressSavedDepthLoc, 0);
            GL13.glActiveTexture(33985);
            int prevTex1 = GL11.glGetInteger(32873);
            int worldDepthTex = worldDepthFbo != null ? worldDepthFbo.getDepthAttachment() : savedSceneDepthFbo.getDepthAttachment();
            GL11.glBindTexture(3553, worldDepthTex);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL20.glUniform1i(uCompressWorldDepthLoc, 1);
            GL20.glUniform1i(uCompressModeLoc, mode);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(519);
            renderFullscreenQuad();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(515);
            GL20.glUseProgram(0);
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, prevTex1);
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, prevTex0);
            RenderSystem.activeTexture(33984);
            GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
            GL30.glBindFramebuffer(36008, prevRead);
            GL30.glBindFramebuffer(36009, prevDraw);
         }
      }
   }

   private static void mergeDepthWithSaved(int width, int height, int mode) {
      tempHandDepthFbo = ensureFbo(tempHandDepthFbo, width, height);
      blitDepthFromCurrent(tempHandDepthFbo);
      initMergeShader();
      if (mergeProgram != 0) {
         int prevRead = GL30.glGetInteger(36010);
         int prevDraw = GL30.glGetInteger(36006);
         int targetFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
         if (targetFbo == 0) {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null) {
               targetFbo = main.fbo;
            }
         }

         if (targetFbo != 0) {
            viewportBuf.clear();
            GL11.glGetIntegerv(2978, viewportBuf);
            GL30.glBindFramebuffer(36160, targetFbo);
            GL11.glViewport(0, 0, width, height);
            GL20.glUseProgram(mergeProgram);
            GL13.glActiveTexture(33984);
            int prevTex0 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, savedSceneDepthFbo.getDepthAttachment());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL20.glUniform1i(uMergeSavedSceneDepthLoc, 0);
            GL13.glActiveTexture(33985);
            int prevTex1 = GL11.glGetInteger(32873);
            GL11.glBindTexture(3553, tempHandDepthFbo.getDepthAttachment());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL20.glUniform1i(uMergeHandDepthLoc, 1);
            GL13.glActiveTexture(33986);
            int prevTex2 = GL11.glGetInteger(32873);
            int worldDepthTex = worldDepthFbo != null ? worldDepthFbo.getDepthAttachment() : savedSceneDepthFbo.getDepthAttachment();
            GL11.glBindTexture(3553, worldDepthTex);
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            GL20.glUniform1i(uMergeWorldDepthLoc, 2);
            GL20.glUniform1i(uMergeModeLoc, mode);
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(519);
            renderFullscreenQuad();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(515);
            GL20.glUseProgram(0);
            GL13.glActiveTexture(33986);
            GL11.glBindTexture(3553, prevTex2);
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, prevTex1);
            GL13.glActiveTexture(33984);
            GL11.glBindTexture(3553, prevTex0);
            RenderSystem.activeTexture(33984);
            GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
            GL30.glBindFramebuffer(36008, prevRead);
            GL30.glBindFramebuffer(36009, prevDraw);
         }
      }
   }

   private static void initCompressShader() {
      if (compressProgram == 0) {
         String vertSrc = "#version 150\nin vec2 Position;\nout vec2 texCoord;\nvoid main() {\n    texCoord = Position * 0.5 + 0.5;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n";
         String fragSrc = "#version 150\nuniform sampler2D SavedSceneDepth;\nuniform sampler2D WorldDepth;\nuniform int Mode;\nin vec2 texCoord;\nvoid main() {\n    float dSaved = texture(SavedSceneDepth, texCoord).r;\n    float dPrepared = 1.0;\n    if (Mode == 0) {\n        dPrepared = (dSaved < 0.99999) ? (dSaved * 0.125 + 0.4375) : 1.0;\n    } else if (Mode == 1) {\n        float dWorld = texture(WorldDepth, texCoord).r;\n        dPrepared = (dWorld < 0.99999) ? (dWorld * 0.125 + 0.4375) : 1.0;\n    } else if (Mode == 2) {\n        float dWorld = texture(WorldDepth, texCoord).r;\n        float dReplay = (dSaved < dWorld - 0.00001) ? dSaved : 1.0;\n        dPrepared = (dReplay < 0.99999) ? (dReplay * 0.125 + 0.4375) : 1.0;\n    } else {\n        dPrepared = 1.0;\n    }\n    gl_FragDepth = dPrepared;\n}\n";
         int vs = GL20.glCreateShader(35633);
         GL20.glShaderSource(vs, vertSrc);
         GL20.glCompileShader(vs);
         int fs = GL20.glCreateShader(35632);
         GL20.glShaderSource(fs, fragSrc);
         GL20.glCompileShader(fs);
         int prog = GL20.glCreateProgram();
         GL20.glAttachShader(prog, vs);
         GL20.glAttachShader(prog, fs);
         GL20.glBindAttribLocation(prog, 0, "Position");
         GL20.glLinkProgram(prog);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         compressProgram = prog;
         uCompressSavedDepthLoc = GL20.glGetUniformLocation(prog, "SavedSceneDepth");
         uCompressWorldDepthLoc = GL20.glGetUniformLocation(prog, "WorldDepth");
         uCompressModeLoc = GL20.glGetUniformLocation(prog, "Mode");
      }
   }

   private static void initMergeShader() {
      if (mergeProgram == 0) {
         String vertSrc = "#version 150\nin vec2 Position;\nout vec2 texCoord;\nvoid main() {\n    texCoord = Position * 0.5 + 0.5;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n";
         String fragSrc = "#version 150\nuniform sampler2D SavedSceneDepth;\nuniform sampler2D HandDepth;\nuniform sampler2D WorldDepth;\nuniform int Mode;\nin vec2 texCoord;\nvoid main() {\n    float dSaved = texture(SavedSceneDepth, texCoord).r;\n    float dHand = texture(HandDepth, texCoord).r;\n\n    if (Mode < 0) {\n        gl_FragDepth = min(dSaved, dHand);\n        return;\n    }\n\n    float dPrepared = 1.0;\n    if (Mode == 0) {\n        dPrepared = (dSaved < 0.99999) ? (dSaved * 0.125 + 0.4375) : 1.0;\n    } else if (Mode == 1) {\n        float dWorld = texture(WorldDepth, texCoord).r;\n        dPrepared = (dWorld < 0.99999) ? (dWorld * 0.125 + 0.4375) : 1.0;\n    } else if (Mode == 2) {\n        float dWorld = texture(WorldDepth, texCoord).r;\n        float dReplay = (dSaved < dWorld - 0.00001) ? dSaved : 1.0;\n        dPrepared = (dReplay < 0.99999) ? (dReplay * 0.125 + 0.4375) : 1.0;\n    } else {\n        dPrepared = 1.0;\n    }\n\n    bool handDrawn = (dHand < dPrepared - 0.00001) && (dHand < 0.5625);\n    gl_FragDepth = handDrawn ? dHand : dSaved;\n}\n";
         int vs = GL20.glCreateShader(35633);
         GL20.glShaderSource(vs, vertSrc);
         GL20.glCompileShader(vs);
         int fs = GL20.glCreateShader(35632);
         GL20.glShaderSource(fs, fragSrc);
         GL20.glCompileShader(fs);
         int prog = GL20.glCreateProgram();
         GL20.glAttachShader(prog, vs);
         GL20.glAttachShader(prog, fs);
         GL20.glBindAttribLocation(prog, 0, "Position");
         GL20.glLinkProgram(prog);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         mergeProgram = prog;
         uMergeSavedSceneDepthLoc = GL20.glGetUniformLocation(prog, "SavedSceneDepth");
         uMergeHandDepthLoc = GL20.glGetUniformLocation(prog, "HandDepth");
         uMergeWorldDepthLoc = GL20.glGetUniformLocation(prog, "WorldDepth");
         uMergeModeLoc = GL20.glGetUniformLocation(prog, "Mode");
      }
   }

   private static void updateReplayOnlyDepth(int width, int height) {
      replayOnlyDepthFbo = ensureFbo(replayOnlyDepthFbo, width, height);
      initReplayOnlyShader();
      if (replayOnlyProgram != 0) {
         int prevRead = GL30.glGetInteger(36010);
         int prevDraw = GL30.glGetInteger(36006);
         viewportBuf.clear();
         GL11.glGetIntegerv(2978, viewportBuf);
         GL30.glBindFramebuffer(36160, replayOnlyDepthFbo.fbo);
         GL11.glViewport(0, 0, width, height);
         GL20.glUseProgram(replayOnlyProgram);
         GL13.glActiveTexture(33984);
         int prevTex0 = GL11.glGetInteger(32873);
         GL11.glBindTexture(3553, worldDepthFbo.getDepthAttachment());
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL20.glUniform1i(uReplayWorldDepthLoc, 0);
         GL13.glActiveTexture(33985);
         int prevTex1 = GL11.glGetInteger(32873);
         GL11.glBindTexture(3553, replayDepthFbo.getDepthAttachment());
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL20.glUniform1i(uReplayFullDepthLoc, 1);
         RenderSystem.colorMask(false, false, false, false);
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.depthFunc(519);
         renderFullscreenQuad();
         RenderSystem.colorMask(true, true, true, true);
         RenderSystem.depthFunc(515);
         GL20.glUseProgram(0);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, prevTex1);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, prevTex0);
         RenderSystem.activeTexture(33984);
         GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
         GL30.glBindFramebuffer(36008, prevRead);
         GL30.glBindFramebuffer(36009, prevDraw);
      }
   }

   private static void initReplayOnlyShader() {
      if (replayOnlyProgram == 0) {
         String vertSrc = "#version 150\nin vec2 Position;\nout vec2 texCoord;\nvoid main() {\n    texCoord = Position * 0.5 + 0.5;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}\n";
         String fragSrc = "#version 150\nuniform sampler2D WorldDepth;\nuniform sampler2D FullDepth;\nin vec2 texCoord;\nvoid main() {\n    float dWorld = texture(WorldDepth, texCoord).r;\n    float dFull = texture(FullDepth, texCoord).r;\n    gl_FragDepth = (dFull < dWorld - 0.00001) ? dFull : 1.0;\n}\n";
         int vs = GL20.glCreateShader(35633);
         GL20.glShaderSource(vs, vertSrc);
         GL20.glCompileShader(vs);
         int fs = GL20.glCreateShader(35632);
         GL20.glShaderSource(fs, fragSrc);
         GL20.glCompileShader(fs);
         int prog = GL20.glCreateProgram();
         GL20.glAttachShader(prog, vs);
         GL20.glAttachShader(prog, fs);
         GL20.glBindAttribLocation(prog, 0, "Position");
         GL20.glLinkProgram(prog);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         replayOnlyProgram = prog;
         uReplayWorldDepthLoc = GL20.glGetUniformLocation(prog, "WorldDepth");
         uReplayFullDepthLoc = GL20.glGetUniformLocation(prog, "FullDepth");
      }
   }

   private static void renderFullscreenQuad() {
      if (quadVao == 0) {
         quadVao = GL30.glGenVertexArrays();
         quadVbo = GL15.glGenBuffers();
         float[] vertices = new float[]{-1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F};
         GL30.glBindVertexArray(quadVao);
         GL15.glBindBuffer(34962, quadVbo);
         GL15.glBufferData(34962, vertices, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 0, 0L);
         GL30.glBindVertexArray(0);
      }

      GL30.glBindVertexArray(quadVao);
      GL11.glDrawArrays(4, 0, 6);
      GL30.glBindVertexArray(0);
   }
}
