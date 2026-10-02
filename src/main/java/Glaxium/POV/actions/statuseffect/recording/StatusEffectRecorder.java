package Glaxium.POV.actions.statuseffect.recording;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.statuseffect.StatusEffectEntry;
import Glaxium.POV.config.PovSettings;
import Glaxium.POV.integration.access.bbs.ReplayKeyframesPovAccess;
import mchorse.bbs_mod.film.Recorder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bakes active vanilla player status effects into StatusEffectsPovActionClips during replay recording.
 */
public final class StatusEffectRecorder
{
    private StatusEffectsPovActionClip recordingClip;
    private Set<String> currentEffectIds = new HashSet<>();

    public void reset()
    {
        this.recordingClip = null;
        this.currentEffectIds.clear();
    }

    public void finish(ReplayKeyframesPovAccess access, int tick)
    {
        this.finalizeClip(tick);
    }

    public void record(ReplayKeyframesPovAccess access, Recorder recorder)
    {
        if (!PovSettings.isBakeStatusEffects())
        {
            return;
        }

        if (recorder.hasNotStarted() || recorder.tick < 0)
        {
            return;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            this.finalizeClip(recorder.tick);
            return;
        }

        Collection<StatusEffectInstance> active = player.getStatusEffects();
        if (active == null || active.isEmpty())
        {
            this.finalizeClip(recorder.tick);
            return;
        }

        Set<String> newEffectIds = new HashSet<>();
        for (StatusEffectInstance inst : active)
        {
            Identifier id = Registries.STATUS_EFFECT.getId(inst.getEffectType());
            if (id != null)
            {
                newEffectIds.add(id.toString());
            }
        }

        if (this.recordingClip != null && !this.currentEffectIds.equals(newEffectIds))
        {
            this.finalizeClip(recorder.tick);
        }

        if (this.recordingClip == null)
        {
            this.recordingClip = (StatusEffectsPovActionClip) access.bbsPov$getActions().add(
                PovActionType.STATUS_EFFECTS, recorder.tick, 1);
            this.currentEffectIds = newEffectIds;

            List<StatusEffectInstance> sortedActive = new ArrayList<>(active);
            sortedActive.sort(null);

            for (StatusEffectInstance inst : sortedActive)
            {
                Identifier id = Registries.STATUS_EFFECT.getId(inst.getEffectType());
                if (id == null) continue;

                boolean unlimited = inst.isInfinite();
                int durationSeconds = unlimited ? 100 : Math.max(1, inst.getDuration() / 20);
                int amp = inst.getAmplifier();

                StatusEffectEntry entry = new StatusEffectEntry(id.toString(), unlimited, durationSeconds, amp);
                this.recordingClip.addEffect(entry);
            }
        }

        float localTick = (float) (recorder.tick - this.recordingClip.tick.get());
        this.recordingClip.duration.set(Math.max(1, (int) localTick + 1));
    }

    private void finalizeClip(int tick)
    {
        if (this.recordingClip == null)
        {
            return;
        }

        this.recordingClip.duration.set(Math.max(1, tick - this.recordingClip.tick.get()));
        this.recordingClip = null;
        this.currentEffectIds.clear();
    }
}
