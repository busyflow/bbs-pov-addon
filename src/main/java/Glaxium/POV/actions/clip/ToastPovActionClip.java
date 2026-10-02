package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.toast.ToastPresets;
import Glaxium.POV.actions.toast.ToastTypeEntry;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;

/** Action clip representing a vanilla Toast popup notification. */
public final class ToastPovActionClip extends PovActionClip
{
    public final ValueString presetId = new ValueString("preset_id", "adv_stone_age");
    public final ValueString customTitle = new ValueString("custom_title", "");
    public final ValueString customDescription = new ValueString("custom_description", "");
    public final ValueString customIcon = new ValueString("custom_icon", "");
    public final ValueLink customTexture = new ValueLink("custom_texture", null);
    public final ValueString frameType = new ValueString("frame_type", "task");

    public ToastPovActionClip()
    {
        super();

        this.add(this.presetId);
        this.add(this.customTitle);
        this.add(this.customDescription);
        this.add(this.customIcon);
        this.add(this.customTexture);
        this.add(this.frameType);
    }

    @Override
    public Clip create()
    {
        ToastPovActionClip clip = new ToastPovActionClip();
        clip.copy(this);
        return clip;
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.TOASTS;
    }

    public void trimToRecording(int endTick)
    {
        int start = this.tick.get();
        if (endTick > start && (start + this.duration.get()) > endTick)
        {
            this.duration.set(Math.max(1, endTick - start));
        }
    }

    public void ensureBakingBounds()
    {
        if (this.duration.get() < 1)
        {
            this.duration.set(1);
        }
    }

    public String getPresetId()
    {
        return this.presetId.get();
    }

    public void setPresetId(String presetId)
    {
        this.presetId.set(presetId == null ? "adv_stone_age" : presetId);
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        if (entry != null)
        {
            this.frameType.set(entry.frameType);
        }
    }

    public void applyPreset(ToastTypeEntry entry)
    {
        if (entry != null)
        {
            this.presetId.set(entry.id);
            this.customTitle.set("");
            this.customDescription.set("");
            this.customIcon.set("");
            this.customTexture.set(null);
            this.frameType.set(entry.frameType);
        }
    }

    public String getEffectiveTitle()
    {
        String title = this.customTitle.get();
        if (title != null && !title.trim().isEmpty())
        {
            return title;
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.title : "Advancement Made!";
    }

    public net.minecraft.text.Text getEffectiveTitleText()
    {
        String title = this.customTitle.get();
        if (title != null && !title.trim().isEmpty())
        {
            return net.minecraft.text.Text.literal(title);
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.getTitleText() : net.minecraft.text.Text.translatable("advancements.toast.task");
    }

    public String getEffectiveDescription()
    {
        String desc = this.customDescription.get();
        if (desc != null && !desc.trim().isEmpty())
        {
            return desc;
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.description : "Stone Age";
    }

    public net.minecraft.text.Text getEffectiveDescriptionText()
    {
        String desc = this.customDescription.get();
        if (desc != null && !desc.trim().isEmpty())
        {
            return net.minecraft.text.Text.literal(desc);
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.getDescriptionText() : net.minecraft.text.Text.translatable("advancements.story.mine_stone.title");
    }

    public String getEffectiveIcon()
    {
        String icon = this.customIcon.get();
        if (icon != null && !icon.trim().isEmpty())
        {
            return icon;
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.iconItemId : "minecraft:wooden_pickaxe";
    }

    public Link getCustomTexture()
    {
        return this.customTexture.get();
    }

    public void setCustomTexture(Link customTexture)
    {
        this.customTexture.set(customTexture);
    }

    public String getEffectiveFrameType()
    {
        String type = this.frameType.get();
        if (type != null && !type.trim().isEmpty())
        {
            return type;
        }
        ToastTypeEntry entry = ToastPresets.getById(this.presetId.get());
        return entry != null ? entry.frameType : "task";
    }

    public String getCustomTitle()
    {
        return this.customTitle.get();
    }

    public void setCustomTitle(String customTitle)
    {
        this.customTitle.set(customTitle == null ? "" : customTitle);
    }

    public String getCustomDescription()
    {
        return this.customDescription.get();
    }

    public void setCustomDescription(String customDescription)
    {
        this.customDescription.set(customDescription == null ? "" : customDescription);
    }

    public String getCustomIcon()
    {
        return this.customIcon.get();
    }

    public void setCustomIcon(String customIcon)
    {
        this.customIcon.set(customIcon == null ? "" : customIcon);
    }

    public String getFrameType()
    {
        return this.frameType.get();
    }

    public void setFrameType(String frameType)
    {
        this.frameType.set(frameType == null ? "task" : frameType);
    }
}
