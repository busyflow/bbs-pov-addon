package Glaxium.POV.actions.chat.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.hud.RecordedHudData;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import Glaxium.POV.integration.mixin.minecraft.ChatScreenPovAccessor;
import Glaxium.POV.integration.mixin.minecraft.TextFieldWidgetPovAccessor;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.Objects;

/** Records Chat Screen opens, typing, cursor position, selection, and hotbar cursor keys into ChatPovActionClip. */
public final class ChatRecorder
{
    private ChatPovActionClip recordingChatClip;
    private String lastText = null;
    private Boolean lastBarVisible = null;
    private Integer lastCursor = null;
    private Integer lastSelStart = null;
    private Integer lastSelEnd = null;
    private Integer lastScroll = null;

    private float lastRecordedCurTx = Float.NaN;
    private float lastRecordedCurTy = Float.NaN;
    private float lastCursorKeyTick = Float.NaN;
    private static ChatRecorder activeInstance = null;
    private static int currentRecorderTick = 0;
    private static float lastExecutedLocalTick = -1F;
    private static int executedTextCounter = 0;
    private ReplayKeyframesPovAccess currentAccess = null;
    private Recorder currentRecorder = null;

    private static boolean replayingExternalMessage = false;

    public static void setReplayingExternalMessage(boolean replaying)
    {
        replayingExternalMessage = replaying;
    }

    public void reset()
    {
        activeInstance = null;
        this.currentAccess = null;
        this.currentRecorder = null;
        this.recordingChatClip = null;
        this.lastText = null;
        this.lastBarVisible = null;
        this.lastCursor = null;
        this.lastSelStart = null;
        this.lastSelEnd = null;
        this.lastScroll = null;
        this.lastRecordedCurTx = Float.NaN;
        this.lastRecordedCurTy = Float.NaN;
        this.lastCursorKeyTick = Float.NaN;
        lastExecutedLocalTick = -1F;
        executedTextCounter = 0;
        replayingExternalMessage = false;
    }

    public static void onChatMessageReceived(net.minecraft.text.Text message)
    {
        if (replayingExternalMessage || activeInstance == null || message == null)
        {
            return;
        }

        if (activeInstance.currentAccess == null || activeInstance.currentRecorder == null)
        {
            return;
        }

        String formatted = toFormattedString(message);
        if (formatted == null || formatted.trim().isEmpty())
        {
            return;
        }

        if (activeInstance.recordingChatClip == null)
        {
            Glaxium.POV.actions.RecordedPovActions actions = activeInstance.currentAccess.bbsPov$getActions();
            if (actions != null)
            {
                for (ChatPovActionClip clip : actions.getClips(ChatPovActionClip.class))
                {
                    if (clip != null)
                    {
                        activeInstance.recordingChatClip = clip;
                        break;
                    }
                }

                if (activeInstance.recordingChatClip == null)
                {
                    activeInstance.recordingChatClip = (ChatPovActionClip) actions.add(PovActionType.CHAT, 0, activeInstance.currentRecorder.tick + 1);
                    activeInstance.recordingChatClip.createDefaultKeyframes();
                    activeInstance.recordingChatClip.barVisible.insert(0F, false);
                }
            }
        }

        if (activeInstance.recordingChatClip == null)
        {
            return;
        }

        float localTick = (float) (currentRecorderTick - activeInstance.recordingChatClip.tick.get());
        if (localTick < 0F)
        {
            localTick = 0F;
        }
        activeInstance.recordingChatClip.duration.set(Math.max(activeInstance.recordingChatClip.duration.get(), (int) localTick + 1));

        String[] lines = formatted.split("\n");
        for (String line : lines)
        {
            if (line != null && !line.trim().isEmpty())
            {
                if (Math.abs(localTick - lastExecutedLocalTick) < 0.0001F)
                {
                    executedTextCounter++;
                }
                else
                {
                    lastExecutedLocalTick = localTick;
                    executedTextCounter = 0;
                }

                float keyTick = localTick + (executedTextCounter * 0.001F);
                activeInstance.recordingChatClip.executedText.insert(keyTick, line);
            }
        }
    }

    public static String toFormattedString(net.minecraft.text.Text text)
    {
        if (text == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        text.visit((style, string) ->
        {
            if (style != null)
            {
                if (style.getColor() != null)
                {
                    net.minecraft.util.Formatting formatting = net.minecraft.util.Formatting.byName(style.getColor().getName());
                    if (formatting != null)
                    {
                        sb.append(formatting.toString());
                    }
                    else
                    {
                        sb.append("§f");
                    }
                }
                if (style.isBold()) sb.append("§l");
                if (style.isItalic()) sb.append("§o");
                if (style.isUnderlined()) sb.append("§n");
                if (style.isStrikethrough()) sb.append("§m");
                if (style.isObfuscated()) sb.append("§k");
            }
            sb.append(string);
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);

        return sb.toString();
    }

    public void finish(ReplayKeyframesPovAccess access, int tick)
    {
        if (this.recordingChatClip != null)
        {
            if (this.lastBarVisible != null && this.lastBarVisible)
            {
                float localTick = (float) (tick - this.recordingChatClip.tick.get());
                this.recordValue(this.recordingChatClip.barVisible, false, localTick, this.lastBarVisible);
                this.lastBarVisible = false;
                this.recordingChatClip.duration.set(Math.max(this.recordingChatClip.duration.get(), (int) localTick + 1));
            }
            this.recordingChatClip.duration.set(Math.max(1, this.recordingChatClip.duration.get()));
            this.recordingChatClip = null;
        }

        RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;
        if (hotbar != null && (this.lastBarVisible != null && this.lastBarVisible))
        {
            if (MinecraftClient.getInstance().currentScreen == null)
            {
                hotbar.cursorVisible.insert((float) tick, false);
            }
        }

        this.currentAccess = null;
        this.currentRecorder = null;
        activeInstance = null;
    }

    public void record(ReplayKeyframesPovAccess access, Recorder recorder)
    {
        if (!PovSettings.isBakeActions())
        {
            return;
        }

        if (recorder.hasNotStarted() || recorder.tick < 0)
        {
            return;
        }

        activeInstance = this;
        currentRecorderTick = recorder.tick;
        this.currentAccess = access;
        this.currentRecorder = recorder;

        Screen screen = MinecraftClient.getInstance().currentScreen;
        RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;

        if (screen instanceof ChatScreen chatScreen)
        {
            if (this.recordingChatClip == null)
            {
                this.recordingChatClip = (ChatPovActionClip) access.bbsPov$getActions().add(PovActionType.CHAT, recorder.tick, 1);
                this.recordingChatClip.createDefaultKeyframes();
                this.lastText = null;
                this.lastBarVisible = null;
                this.lastCursor = null;
                this.lastSelStart = null;
                this.lastSelEnd = null;

                if (hotbar != null)
                {
                    hotbar.cursorVisible.insert((float) recorder.tick, true);
                }
            }

            float localTick = (float) (recorder.tick - this.recordingChatClip.tick.get());
            this.recordingChatClip.duration.set(Math.max(1, (int) localTick + 1));

            TextFieldWidget field = ((ChatScreenPovAccessor) chatScreen).bbsPov$getChatField();
            if (field != null)
            {
                String text = field.getText();
                int cursor = field.getCursor();
                int selStart = ((TextFieldWidgetPovAccessor) field).bbsPov$getSelectionStart();
                int selEnd = ((TextFieldWidgetPovAccessor) field).bbsPov$getSelectionEnd();

                if (this.lastBarVisible == null || !this.lastBarVisible)
                {
                    this.recordValue(this.recordingChatClip.barVisible, true, localTick, this.lastBarVisible);
                    this.lastBarVisible = true;
                    if (hotbar != null)
                    {
                        hotbar.cursorVisible.insert((float) recorder.tick, true);
                    }
                }

                this.recordValue(this.recordingChatClip.text, text, localTick, this.lastText);
                this.lastText = text;

                this.recordValue(this.recordingChatClip.cursorPos, cursor, localTick, this.lastCursor);
                this.lastCursor = cursor;

                this.recordValue(this.recordingChatClip.selStart, selStart, localTick, this.lastSelStart);
                this.lastSelStart = selStart;

                this.recordValue(this.recordingChatClip.selEnd, selEnd, localTick, this.lastSelEnd);
                this.lastSelEnd = selEnd;

                int scroll = 0;
                if (MinecraftClient.getInstance().inGameHud != null && MinecraftClient.getInstance().inGameHud.getChatHud() != null)
                {
                    scroll = ((Glaxium.POV.integration.mixin.minecraft.ChatHudPovAccessor) MinecraftClient.getInstance().inGameHud.getChatHud()).bbsPov$getScrolledLines();
                }
                this.recordValue(this.recordingChatClip.chatScroll, scroll, localTick, this.lastScroll);
                this.lastScroll = scroll;
            }
        }
        else if (this.recordingChatClip != null)
        {
            if (this.lastBarVisible == null || this.lastBarVisible)
            {
                float localTick = (float) (recorder.tick - this.recordingChatClip.tick.get());
                this.recordValue(this.recordingChatClip.barVisible, false, localTick, this.lastBarVisible);
                this.lastBarVisible = false;
                this.recordingChatClip.duration.set(Math.max(this.recordingChatClip.duration.get(), (int) localTick + 1));
                if (hotbar != null && MinecraftClient.getInstance().currentScreen == null)
                {
                    hotbar.cursorVisible.insert((float) recorder.tick, false);
                }
            }
        }
    }

    public void sampleCursor(ReplayKeyframesPovAccess access, Recorder recorder, float tickDelta)
    {
        if (this.recordingChatClip == null || recorder.hasNotStarted() || recorder.tick < 0)
        {
            return;
        }

        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof ChatScreen))
        {
            return;
        }

        RecordedHudData hotbar = access != null ? access.bbsPov$getHud() : null;
        if (hotbar == null)
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        double mouseX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();
        double screenCenterX = mc.getWindow().getScaledWidth() / 2.0;
        double screenCenterY = mc.getWindow().getScaledHeight() / 2.0;
        float curTx = (float) ((mouseX - screenCenterX) / HotbarLayoutTransform.PIXELS_PER_UNIT);
        float curTy = (float) ((screenCenterY - mouseY) / HotbarLayoutTransform.PIXELS_PER_UNIT);

        float fraction = Math.max(0F, Math.min(1F, tickDelta));
        float absTick = recorder.tick + fraction;

        KeyframeChannel<Transform> hudCursorLayout = hotbar.cursorLayout;

        if (Float.isNaN(this.lastCursorKeyTick))
        {
            if (hudCursorLayout != null && !hudCursorLayout.getKeyframes().isEmpty())
            {
                int lastIdx = hudCursorLayout.getKeyframes().size() - 1;
                hudCursorLayout.getKeyframes().get(lastIdx).getInterpolation().setInterp(Interpolations.CONST);
            }
            hotbar.cursorVisible.insert(absTick, true);
            this.insertCursorKey(hudCursorLayout, absTick, curTx, curTy);
            this.lastRecordedCurTx = curTx;
            this.lastRecordedCurTy = curTy;
            this.lastCursorKeyTick = absTick;
        }
        else if (absTick > this.lastCursorKeyTick + 0.35F
            && (Math.abs(curTx - this.lastRecordedCurTx) > 0.03F
                || Math.abs(curTy - this.lastRecordedCurTy) > 0.03F))
        {
            if (absTick - this.lastCursorKeyTick > 0.8F)
            {
                this.insertCursorKey(
                    hudCursorLayout,
                    absTick - 0.1F,
                    this.lastRecordedCurTx,
                    this.lastRecordedCurTy);
            }

            this.insertCursorKey(hudCursorLayout, absTick, curTx, curTy);
            this.lastRecordedCurTx = curTx;
            this.lastRecordedCurTy = curTy;
            this.lastCursorKeyTick = absTick;
        }
    }

    private void insertCursorKey(KeyframeChannel<Transform> channel, float tick, float x, float y)
    {
        if (channel == null)
        {
            return;
        }

        Transform transform = new Transform();
        transform.translate.set(x, y, 0F);
        int index = channel.insert(tick, transform);
        if (index >= 0 && index < channel.getKeyframes().size())
        {
            channel.getKeyframes().get(index)
                .getInterpolation().setInterp(Interpolations.LINEAR);
        }
    }

    private <T> void recordValue(KeyframeChannel<T> channel, T value, float tick, T last)
    {
        if (channel.isEmpty() || !Objects.equals(value, last))
        {
            channel.insert(tick, value);
        }
    }
}
