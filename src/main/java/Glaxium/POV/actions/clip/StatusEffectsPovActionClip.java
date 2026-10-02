package Glaxium.POV.actions.clip;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.statuseffect.StatusEffectEntry;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatusEffectsPovActionClip extends PovActionClip
{
    private final List<StatusEffectEntry> effects = new ArrayList<>();

    public StatusEffectsPovActionClip()
    {
        super();
    }

    @Override
    public PovActionType getActionType()
    {
        return PovActionType.STATUS_EFFECTS;
    }

    public List<StatusEffectEntry> getEffects()
    {
        return this.effects;
    }

    public void addEffect(StatusEffectEntry entry)
    {
        if (entry != null && !this.hasEffect(entry.getEffectId()))
        {
            this.effects.add(entry);
        }
    }

    public void removeEffect(StatusEffectEntry entry)
    {
        this.effects.remove(entry);
    }

    public boolean hasEffect(String effectId)
    {
        if (effectId == null)
        {
            return false;
        }
        for (StatusEffectEntry entry : this.effects)
        {
            if (effectId.equalsIgnoreCase(entry.getEffectId()))
            {
                return true;
            }
        }
        return false;
    }

    public void moveUp(int index)
    {
        if (index > 0 && index < this.effects.size())
        {
            Collections.swap(this.effects, index, index - 1);
        }
    }

    public void moveDown(int index)
    {
        if (index >= 0 && index < this.effects.size() - 1)
        {
            Collections.swap(this.effects, index, index + 1);
        }
    }

    @Override
    public BaseType toData()
    {
        BaseType baseData = super.toData();
        MapType data = baseData instanceof MapType ? (MapType) baseData : new MapType();

        ListType list = new ListType();
        for (StatusEffectEntry entry : this.effects)
        {
            MapType entryData = new MapType();
            entry.toData(entryData);
            list.add(entryData);
        }
        data.put("status_effects", list);
        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        this.effects.clear();
        if (data instanceof MapType map && map.has("status_effects") && map.get("status_effects") instanceof ListType list)
        {
            for (BaseType element : list)
            {
                if (element instanceof MapType entryMap)
                {
                    StatusEffectEntry entry = new StatusEffectEntry();
                    entry.fromData(entryMap);
                    this.effects.add(entry);
                }
            }
        }
    }

    @Override
    protected Clip create()
    {
        StatusEffectsPovActionClip clip = new StatusEffectsPovActionClip();
        for (StatusEffectEntry entry : this.effects)
        {
            clip.addEffect(entry.copy());
        }
        return clip;
    }
}
