package Glaxium.POV.actions.bossbar;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Vanilla boss-bar color / notch ids and GUI sprites. */
public final class BossBarLooks
{
    public static final String[] COLORS = {
        "pink", "blue", "red", "green", "yellow", "purple", "white"
    };
    public static final String[] STYLES = {
        "progress", "notched_6", "notched_10", "notched_12", "notched_20"
    };

    private static final Identifier[] BACKGROUNDS = new Identifier[COLORS.length];
    private static final Identifier[] PROGRESS = new Identifier[COLORS.length];
    private static final Identifier[] NOTCHED_BACKGROUNDS = {
        new Identifier("boss_bar/notched_6_background"),
        new Identifier("boss_bar/notched_10_background"),
        new Identifier("boss_bar/notched_12_background"),
        new Identifier("boss_bar/notched_20_background")
    };
    private static final Identifier[] NOTCHED_PROGRESS = {
        new Identifier("boss_bar/notched_6_progress"),
        new Identifier("boss_bar/notched_10_progress"),
        new Identifier("boss_bar/notched_12_progress"),
        new Identifier("boss_bar/notched_20_progress")
    };

    static
    {
        for (int i = 0; i < COLORS.length; i++)
        {
            BACKGROUNDS[i] = new Identifier("boss_bar/" + COLORS[i] + "_background");
            PROGRESS[i] = new Identifier("boss_bar/" + COLORS[i] + "_progress");
        }
    }

    private BossBarLooks()
    {
    }

    public static String colorId(BossBar.Color color)
    {
        return color == null ? "pink" : color.getName();
    }

    public static String styleId(BossBar.Style style)
    {
        return style == null ? "progress" : style.getName();
    }

    public static int colorIndex(String id)
    {
        String key = normalize(id, COLORS[0]);
        for (int i = 0; i < COLORS.length; i++)
        {
            if (COLORS[i].equals(key))
            {
                return i;
            }
        }

        return 0;
    }

    public static int styleIndex(String id)
    {
        String key = normalize(id, STYLES[0]);
        for (int i = 0; i < STYLES.length; i++)
        {
            if (STYLES[i].equals(key))
            {
                return i;
            }
        }

        return 0;
    }

    public static String nextColor(String id)
    {
        return COLORS[(colorIndex(id) + 1) % COLORS.length];
    }

    public static String nextStyle(String id)
    {
        return STYLES[(styleIndex(id) + 1) % STYLES.length];
    }

    public static String displayColor(String id)
    {
        String key = COLORS[colorIndex(id)];
        return Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    public static String displayStyle(String id)
    {
        return switch (styleIndex(id))
        {
            case 1 -> "Notched 6";
            case 2 -> "Notched 10";
            case 3 -> "Notched 12";
            case 4 -> "Notched 20";
            default -> "Progress";
        };
    }

    public static Identifier background(String color)
    {
        return BACKGROUNDS[colorIndex(color)];
    }

    public static Identifier progress(String color)
    {
        return PROGRESS[colorIndex(color)];
    }

    public static Identifier notchedBackground(String style)
    {
        int index = styleIndex(style) - 1;
        return index < 0 ? null : NOTCHED_BACKGROUNDS[index];
    }

    public static Identifier notchedProgress(String style)
    {
        int index = styleIndex(style) - 1;
        return index < 0 ? null : NOTCHED_PROGRESS[index];
    }

    private static String normalize(String id, String fallback)
    {
        if (id == null || id.isBlank())
        {
            return fallback;
        }

        return id.toLowerCase(Locale.ROOT).trim();
    }
}
