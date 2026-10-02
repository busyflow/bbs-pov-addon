package wemppy.bbs_pov.clips;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;

public class ScreenEffectsClip extends CameraClip
{
    public final ValueString effectType = new ValueString("effect_type", "totem");
    public final ValueItemStack item = new ValueItemStack("item");
    public final ValueBoolean showParticles = new ValueBoolean("show_particles", true);
    public final ValueBoolean playSound = new ValueBoolean("play_sound", true);
    public final ValueFloat scaleMultiplier = new ValueFloat("scale_multiplier", 1.0F, 0.1F, 10.0F);

    public final KeyframeChannel<ItemStack> itemChannel = new KeyframeChannel<>("item_channel", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<Double> progressChannel = new KeyframeChannel<>("progress", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> scaleChannel = new KeyframeChannel<>("scale", KeyframeFactories.DOUBLE);

    public ScreenEffectsClip()
    {
        super();

        this.item.set(new ItemStack(Items.TOTEM_OF_UNDYING));

        this.add(this.effectType);
        this.add(this.item);
        this.add(this.showParticles);
        this.add(this.playSound);
        this.add(this.scaleMultiplier);

        this.add(this.itemChannel);
        this.add(this.progressChannel);
        this.add(this.scaleChannel);
    }

    public ItemStack getItemAt(float tick)
    {
        if (!this.itemChannel.isEmpty())
        {
            ItemStack stack = this.itemChannel.interpolate(tick);
            if (stack != null && !stack.isEmpty())
            {
                return stack;
            }
        }
        ItemStack current = this.item.get();
        return current == null || current.isEmpty() ? new ItemStack(Items.TOTEM_OF_UNDYING) : current;
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float relTick = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), relTick);

        if (factor <= 0F)
        {
            return;
        }

        ItemStack activeItem = this.getItemAt(relTick);
        float scale = this.scaleMultiplier.get();
        if (!this.scaleChannel.isEmpty())
        {
            scale *= this.scaleChannel.interpolate(relTick).floatValue();
        }

        float progress = relTick / Math.max(1.0F, (float) this.duration.get());
        if (!this.progressChannel.isEmpty())
        {
            progress = this.progressChannel.interpolate(relTick).floatValue();
        }

        ScreenEffectsData data = new ScreenEffectsData(
            this.effectType.get(),
            activeItem,
            progress,
            relTick,
            this.duration.get(),
            factor,
            scale,
            this.showParticles.get(),
            this.playSound.get(),
            context.ticks
        );

        context.clipData.get("bbs_screen_effects", ArrayList::new).add(data);
    }

    @Override
    protected Clip create()
    {
        return new ScreenEffectsClip();
    }
}
