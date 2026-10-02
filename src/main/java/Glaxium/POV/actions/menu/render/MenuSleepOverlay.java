package Glaxium.POV.actions.menu.render;

/**
 * Vanilla {@code InGameHud} sleep overlay: navy {@code 0x101020} with alpha
 * {@code 220 * progress}. Progress rises with {@code sleepTimer / 100} while
 * in bed, then fades over 10 ticks after waking ({@code 100..110}).
 */
public final class MenuSleepOverlay
{
    public static final int RGB = 0x101020;
    public static final float MAX_ALPHA = 220F;

    private MenuSleepOverlay()
    {
    }

    public static float progress(int sleepTimer)
    {
        if (sleepTimer <= 0)
        {
            return 0F;
        }

        float timer = sleepTimer;
        float progress = timer / 100F;
        if (progress > 1F)
        {
            progress = 1F - (timer - 100F) / 10F;
        }

        return Math.max(0F, Math.min(1F, progress));
    }

    public static int color(float progress)
    {
        float clamped = Math.max(0F, Math.min(1F, progress));
        int alpha = (int) (MAX_ALPHA * clamped);
        return (alpha << 24) | RGB;
    }
}
