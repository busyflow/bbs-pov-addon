package Glaxium.POV.actions;

import java.util.Locale;

/** Stable serialized identifiers for POV-owned, non-hand visual actions. */
public enum PovActionType
{
    GUI("gui", "GUI"),
    MENU("menu", "Menu"),
    CAMERA_SHAKE("camera_shake", "Camera Shake"),
    PARTICLE_EFFECT("particle_effect", "Particle Effect"),
    BOSS_BARS("boss_bars", "Boss Bars"),
    STATUS_EFFECTS("status_effects", "Status Effects"),
    TOASTS("toasts", "Toasts"),
    CHAT("chat", "Chat"),
    SCREEN_EFFECT("screen_effect", "Screen Effect");

    public final String id;
    public final String title;

    PovActionType(String id, String title)
    {
        this.id = id;
        this.title = title;
    }

    /** Dedicated timeline rows: GUI 0, Menu 1, Camera Shake 2, Chat 3, Status Effects 4, Screen Effect 5, Particle Effect 6, Toasts 7..11, Boss Bars 12..20. */
    public int seedLayer()
    {
        return switch (this)
        {
            case GUI -> 0;
            case MENU -> 1;
            case CAMERA_SHAKE -> 2;
            case CHAT -> 3;
            case STATUS_EFFECTS -> 4;
            case SCREEN_EFFECT -> 5;
            case PARTICLE_EFFECT -> 6;
            case TOASTS -> 7;
            case BOSS_BARS -> 12;
        };
    }

    public static PovActionType fromId(String id)
    {
        if (id != null)
        {
            if ("particle".equals(id.toLowerCase(Locale.ROOT))
                || "eating_effect".equals(id.toLowerCase(Locale.ROOT)))
            {
                return PARTICLE_EFFECT;
            }

            for (PovActionType type : values())
            {
                if (type.id.equals(id.toLowerCase(Locale.ROOT)))
                {
                    return type;
                }
            }
        }

        return GUI;
    }
}
