package wemppy.bbs_pov.clips;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;

public class AdvancementToastClip extends CameraClip
{
    public final ValueString title = new ValueString("title", "Goal Reached!");
    public final ValueString description = new ValueString("description", "Postmortal");
    public final ValueItemStack icon = new ValueItemStack("icon");
    public final ValueString banner = new ValueString("banner", "1 was saved!");

    public AdvancementToastClip()
    {
        super();

        this.icon.set(new ItemStack(Items.TOTEM_OF_UNDYING));

        this.add(this.title);
        this.add(this.description);
        this.add(this.icon);
        this.add(this.banner);
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

        ItemStack itemIcon = this.icon.get();
        if (itemIcon == null || itemIcon.isEmpty())
        {
            itemIcon = new ItemStack(Items.TOTEM_OF_UNDYING);
        }

        ToastData data = new ToastData(
            this.title.get(),
            this.description.get(),
            itemIcon,
            this.banner.get(),
            factor,
            relTick,
            this.duration.get()
        );

        context.clipData.get("bbs_advancement_toasts", ArrayList::new).add(data);
    }

    @Override
    protected Clip create()
    {
        return new AdvancementToastClip();
    }
}
