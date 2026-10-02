package Glaxium.POV.hand.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

/** Manages depth buffers for World Interaction and Replay Interaction toggles. */
public final class PovHandDepthManager
{
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

    private PovHandDepthManager()
    {
    }

    public static void onRenderWorldStart()
    {
        hasCapturedWorldDepth = false;
        hasCapturedReplayDepth = false;
        hasSavedSceneDepth = false;
    }

    public static void onBeforeReplayRender()
    {
        if (!hasCapturedWorldDepth)
        {
            captureWorldDepth();
            hasCapturedWorldDepth = true;
        }
    }

    public static void onRenderWorldEnd()
    {
        if (!hasCapturedWorldDepth)
        {
            captureWorldDepth();
            hasCapturedWorldDepth = true;
        }
    }

    private static int getRenderWidth()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null && client.getFramebuffer().textureWidth > 0)
        {
            return client.getFramebuffer().textureWidth;
        }
        return client.getWindow().getFramebufferWidth();
    }

    private static int getRenderHeight()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null && client.getFramebuffer().textureHeight > 0)
        {
            return client.getFramebuffer().textureHeight;
        }
        return client.getWindow().getFramebufferHeight();
    }

    private static Framebuffer ensureFbo(Framebuffer current, int width, int height)
    {
        if (current == null)
        {
            current = new SimpleFramebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
            current.setTexFilter(GL11.GL_NEAREST);
        }
        else if (current.textureWidth != width || current.textureHeight != height)
        {
            current.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
            current.setTexFilter(GL11.GL_NEAREST);
        }
        return current;
    }

    public static void captureWorldDepth()
    {
        int width = getRenderWidth();
        int height = getRenderHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        worldDepthFbo = ensureFbo(worldDepthFbo, width, height);
        blitDepthFromCurrent(worldDepthFbo);
        hasCapturedWorldDepth = true;
    }

    public static void captureReplayDepth()
    {
        int width = getRenderWidth();
        int height = getRenderHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        replayDepthFbo = ensureFbo(replayDepthFbo, width, height);
        blitDepthFromCurrent(replayDepthFbo);
        hasCapturedReplayDepth = true;
    }

    public static boolean hasSavedSceneDepth()
    {
        return hasSavedSceneDepth;
    }

    public static void prepareIrisHandDepth(boolean worldInteraction, boolean replayInteraction)
    {
        int width = getRenderWidth();
        int height = getRenderHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
        blitDepthFromCurrent(savedSceneDepthFbo);
        hasSavedSceneDepth = true;

        int mode = 0;
        if (worldInteraction && replayInteraction)
        {
            mode = 0;
        }
        else if (worldInteraction && !replayInteraction)
        {
            mode = 1;
        }
        else if (!worldInteraction && replayInteraction)
        {
            mode = 2;
        }
        else
        {
            mode = 3;
        }

        compressDepthToCurrent(width, height, mode);
    }

    public static void endIrisHandDepth(boolean worldInteraction, boolean replayInteraction)
    {
        if (!hasSavedSceneDepth || savedSceneDepthFbo == null)
        {
            return;
        }
        hasSavedSceneDepth = false;

        int width = getRenderWidth();
        int height = getRenderHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        int mode = 0;
        if (worldInteraction && replayInteraction)
        {
            mode = 0;
        }
        else if (worldInteraction && !replayInteraction)
        {
            mode = 1;
        }
        else if (!worldInteraction && replayInteraction)
        {
            mode = 2;
        }
        else
        {
            mode = 3;
        }

        mergeDepthWithSaved(width, height, mode);
    }

    public static void prepareDepthForHand(boolean worldInteraction, boolean replayInteraction)
    {
        if (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled())
        {
            return;
        }
        beginHandDepth(worldInteraction, replayInteraction);
    }

    public static void beginHandDepth(boolean worldInteraction, boolean replayInteraction)
    {
        if (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled())
        {
            return;
        }

        if (!hasCapturedWorldDepth)
        {
            captureWorldDepth();
            hasCapturedWorldDepth = true;
        }

        int width = getRenderWidth();
        int height = getRenderHeight();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        if (!worldInteraction && !replayInteraction)
        {
            // Case 1: Both disabled -> save current depth, then clear depth so hand renders on top
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        }
        else if (worldInteraction && replayInteraction)
        {
            // Case 2: Both enabled -> save current scene depth, then restore captured replay/world depth
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;
            if (hasCapturedReplayDepth && replayDepthFbo != null)
            {
                blitDepthToCurrent(replayDepthFbo);
            }
            else if (hasCapturedWorldDepth && worldDepthFbo != null)
            {
                blitDepthToCurrent(worldDepthFbo);
            }
        }
        else if (worldInteraction && !replayInteraction)
        {
            // Case 3: World only -> save current full scene depth, then restore world depth buffer
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;

            if (hasCapturedWorldDepth && worldDepthFbo != null)
            {
                blitDepthToCurrent(worldDepthFbo);
            }
            else
            {
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            }
        }
        else if (!worldInteraction && replayInteraction)
        {
            // Case 4: Replay only -> save current full scene depth, then prepare replay-only depth
            savedSceneDepthFbo = ensureFbo(savedSceneDepthFbo, width, height);
            blitDepthFromCurrent(savedSceneDepthFbo);
            hasSavedSceneDepth = true;

            if (hasCapturedWorldDepth && worldDepthFbo != null && hasCapturedReplayDepth && replayDepthFbo != null)
            {
                updateReplayOnlyDepth(width, height);
                blitDepthToCurrent(replayOnlyDepthFbo);
            }
            else if (hasCapturedReplayDepth && replayDepthFbo != null)
            {
                blitDepthToCurrent(replayDepthFbo);
            }
            else
            {
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            }
        }
    }

    public static void endHandDepth(boolean worldInteraction, boolean replayInteraction)
    {
        if (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled())
        {
            return;
        }

        if (!hasSavedSceneDepth || savedSceneDepthFbo == null)
        {
            return;
        }
        hasSavedSceneDepth = false;

        int width = getRenderWidth();
        int height = getRenderHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        mergeDepthWithSaved(width, height, -1);
    }

    private static void blitDepthFromCurrent(Framebuffer dst)
    {
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int srcFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
        if (srcFbo == 0)
        {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null)
            {
                srcFbo = main.fbo;
            }
        }

        if (srcFbo == 0 || srcFbo == dst.fbo)
        {
            return;
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, srcFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dst.fbo);
        GL30.glBlitFramebuffer(
            0, 0, dst.textureWidth, dst.textureHeight,
            0, 0, dst.textureWidth, dst.textureHeight,
            GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
        );

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private static void blitDepthToCurrent(Framebuffer src)
    {
        if (src == null)
        {
            return;
        }

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int dstFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
        if (dstFbo == 0)
        {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null)
            {
                dstFbo = main.fbo;
            }
        }

        if (dstFbo == 0 || dstFbo == src.fbo)
        {
            return;
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, src.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, dstFbo);
        GL30.glBlitFramebuffer(
            0, 0, src.textureWidth, src.textureHeight,
            0, 0, src.textureWidth, src.textureHeight,
            GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
        );

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private static void compressDepthToCurrent(int width, int height, int mode)
    {
        initCompressShader();
        if (compressProgram == 0)
        {
            return;
        }

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int targetFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
        if (targetFbo == 0)
        {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null)
            {
                targetFbo = main.fbo;
            }
        }

        if (targetFbo == 0)
        {
            return;
        }

        viewportBuf.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuf);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, targetFbo);
        GL11.glViewport(0, 0, width, height);

        GL20.glUseProgram(compressProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedSceneDepthFbo.getDepthAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uCompressSavedDepthLoc, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int worldDepthTex = (worldDepthFbo != null) ? worldDepthFbo.getDepthAttachment() : savedSceneDepthFbo.getDepthAttachment();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, worldDepthTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uCompressWorldDepthLoc, 1);

        GL20.glUniform1i(uCompressModeLoc, mode);

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        renderFullscreenQuad();

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        GL20.glUseProgram(0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private static void mergeDepthWithSaved(int width, int height, int mode)
    {
        tempHandDepthFbo = ensureFbo(tempHandDepthFbo, width, height);
        blitDepthFromCurrent(tempHandDepthFbo);

        initMergeShader();
        if (mergeProgram == 0)
        {
            return;
        }

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int targetFbo = prevDraw != 0 ? prevDraw : (prevRead != 0 ? prevRead : 0);
        if (targetFbo == 0)
        {
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            if (main != null)
            {
                targetFbo = main.fbo;
            }
        }

        if (targetFbo == 0)
        {
            return;
        }

        viewportBuf.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuf);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, targetFbo);
        GL11.glViewport(0, 0, width, height);

        GL20.glUseProgram(mergeProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedSceneDepthFbo.getDepthAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uMergeSavedSceneDepthLoc, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tempHandDepthFbo.getDepthAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uMergeHandDepthLoc, 1);

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        int prevTex2 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int worldDepthTex = (worldDepthFbo != null) ? worldDepthFbo.getDepthAttachment() : savedSceneDepthFbo.getDepthAttachment();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, worldDepthTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uMergeWorldDepthLoc, 2);

        GL20.glUniform1i(uMergeModeLoc, mode);

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        renderFullscreenQuad();

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        GL20.glUseProgram(0);

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex2);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private static void initCompressShader()
    {
        if (compressProgram != 0)
        {
            return;
        }

        String vertSrc = """
            #version 150
            in vec2 Position;
            out vec2 texCoord;
            void main() {
                texCoord = Position * 0.5 + 0.5;
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

        String fragSrc = """
            #version 150
            uniform sampler2D SavedSceneDepth;
            uniform sampler2D WorldDepth;
            uniform int Mode;
            in vec2 texCoord;
            void main() {
                float dSaved = texture(SavedSceneDepth, texCoord).r;
                float dPrepared = 1.0;
                if (Mode == 0) {
                    dPrepared = (dSaved < 0.99999) ? (dSaved * 0.125 + 0.4375) : 1.0;
                } else if (Mode == 1) {
                    float dWorld = texture(WorldDepth, texCoord).r;
                    dPrepared = (dWorld < 0.99999) ? (dWorld * 0.125 + 0.4375) : 1.0;
                } else if (Mode == 2) {
                    float dWorld = texture(WorldDepth, texCoord).r;
                    float dReplay = (dSaved < dWorld - 0.00001) ? dSaved : 1.0;
                    dPrepared = (dReplay < 0.99999) ? (dReplay * 0.125 + 0.4375) : 1.0;
                } else {
                    dPrepared = 1.0;
                }
                gl_FragDepth = dPrepared;
            }
            """;

        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertSrc);
        GL20.glCompileShader(vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
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

    private static void initMergeShader()
    {
        if (mergeProgram != 0)
        {
            return;
        }

        String vertSrc = """
            #version 150
            in vec2 Position;
            out vec2 texCoord;
            void main() {
                texCoord = Position * 0.5 + 0.5;
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

        String fragSrc = """
            #version 150
            uniform sampler2D SavedSceneDepth;
            uniform sampler2D HandDepth;
            uniform sampler2D WorldDepth;
            uniform int Mode;
            in vec2 texCoord;
            void main() {
                float dSaved = texture(SavedSceneDepth, texCoord).r;
                float dHand = texture(HandDepth, texCoord).r;

                if (Mode < 0) {
                    gl_FragDepth = min(dSaved, dHand);
                    return;
                }

                float dPrepared = 1.0;
                if (Mode == 0) {
                    dPrepared = (dSaved < 0.99999) ? (dSaved * 0.125 + 0.4375) : 1.0;
                } else if (Mode == 1) {
                    float dWorld = texture(WorldDepth, texCoord).r;
                    dPrepared = (dWorld < 0.99999) ? (dWorld * 0.125 + 0.4375) : 1.0;
                } else if (Mode == 2) {
                    float dWorld = texture(WorldDepth, texCoord).r;
                    float dReplay = (dSaved < dWorld - 0.00001) ? dSaved : 1.0;
                    dPrepared = (dReplay < 0.99999) ? (dReplay * 0.125 + 0.4375) : 1.0;
                } else {
                    dPrepared = 1.0;
                }

                bool handDrawn = (dHand < dPrepared - 0.00001) && (dHand < 0.5625);
                gl_FragDepth = handDrawn ? dHand : dSaved;
            }
            """;

        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertSrc);
        GL20.glCompileShader(vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
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

    private static void updateReplayOnlyDepth(int width, int height)
    {
        replayOnlyDepthFbo = ensureFbo(replayOnlyDepthFbo, width, height);

        initReplayOnlyShader();
        if (replayOnlyProgram == 0)
        {
            return;
        }

        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        viewportBuf.clear();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewportBuf);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, replayOnlyDepthFbo.fbo);
        GL11.glViewport(0, 0, width, height);

        GL20.glUseProgram(replayOnlyProgram);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, worldDepthFbo.getDepthAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uReplayWorldDepthLoc, 0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, replayDepthFbo.getDepthAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL20.glUniform1i(uReplayFullDepthLoc, 1);

        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        renderFullscreenQuad();

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        GL20.glUseProgram(0);

        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex1);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        GL11.glViewport(viewportBuf.get(0), viewportBuf.get(1), viewportBuf.get(2), viewportBuf.get(3));
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
    }

    private static void initReplayOnlyShader()
    {
        if (replayOnlyProgram != 0)
        {
            return;
        }

        String vertSrc = """
            #version 150
            in vec2 Position;
            out vec2 texCoord;
            void main() {
                texCoord = Position * 0.5 + 0.5;
                gl_Position = vec4(Position, 0.0, 1.0);
            }
            """;

        String fragSrc = """
            #version 150
            uniform sampler2D WorldDepth;
            uniform sampler2D FullDepth;
            in vec2 texCoord;
            void main() {
                float dWorld = texture(WorldDepth, texCoord).r;
                float dFull = texture(FullDepth, texCoord).r;
                gl_FragDepth = (dFull < dWorld - 0.00001) ? dFull : 1.0;
            }
            """;

        int vs = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vs, vertSrc);
        GL20.glCompileShader(vs);

        int fs = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
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

    private static void renderFullscreenQuad()
    {
        if (quadVao == 0)
        {
            quadVao = GL30.glGenVertexArrays();
            quadVbo = GL15.glGenBuffers();

            float[] vertices = new float[] {
                -1.0f, -1.0f,
                 1.0f, -1.0f,
                 1.0f,  1.0f,
                -1.0f, -1.0f,
                 1.0f,  1.0f,
                -1.0f,  1.0f
            };

            GL30.glBindVertexArray(quadVao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);
            GL30.glBindVertexArray(0);
        }

        GL30.glBindVertexArray(quadVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }
}
