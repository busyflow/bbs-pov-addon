package Glaxium.POV.hud;

import net.minecraft.item.ItemStack;
import mchorse.bbs_mod.utils.pose.Transform;

/**
 * Ported from BBS-CML-EDITION (which doesn't exist in vanilla bbs-mod) by BBS-POV, kept
 * entirely in this addon's own package -- it only USES vanilla bbs-mod's public API, it isn't
 * part of bbs-mod. RecordedHudData produces this per-frame snapshot and HudRenderer
 * draws it.
 */
public class HudState
{
    public static final int HEART_NORMAL = 0;
    public static final int HEART_POISONED = 1;
    public static final int HEART_WITHERED = 2;
    public static final int HEART_ABSORBING = 3;
    public static final int HEART_FROZEN = 4;

    public final ItemStack[] items = new ItemStack[9];
    public boolean visible = true;
    public boolean statusBarsVisible = true;
    public boolean crosshair;
    public boolean cursorVisible = false;
    public final Transform cursorLayout = new Transform();
    public ItemStack cursorItem = ItemStack.EMPTY;
    public ItemStack offhandItem = ItemStack.EMPTY;
    public int selectedSlot;
    public int heartType;
    public boolean hardcore;
    public boolean heartRegeneration;
    public boolean hungerEffect;
    public float health;
    /** Health anchor rendered with Minecraft's pale blinking-heart sprites. */
    public float previousHealth;
    /**
     * Health one tick earlier on the same actor keyframe curve. Used purely
     * to drive the vanilla-style "hurt/heal flash" on the hearts that changed -- NOT a general
     * "previous frame" value, so it stays correct even when scrubbing the timeline backwards/
     * jumping around, unlike a mutable field that only makes sense during forward playback.
     */
    public float lastHealth;

    /** Recent health bounds retained to reconstruct Previous Health in legacy films. */
    public float recentHealthLow;

    /** @see #recentHealthLow */
    public float recentHealthHigh;

    public float healthContainer;
    public float absorption;

    /**
     * Lowest and highest {@code absorption} value seen over the last
     * recent ticks (including now) -- same rolling-window idea
     * as {@link #recentHealthLow}/{@link #recentHealthHigh}, but tracking the golden/absorption
     * hearts' own curve instead of regular health's. Golden hearts should only ever flash when
     * they've genuinely just gone UP (e.g. eating a golden apple) -- taking damage should make
     * them disappear silently, with no flash at all. See HudRenderer#renderBar's absorption
     * call, which uses this range instead of {@link #absorption} duplicated on both ends (which
     * would make the "recently increased" check always false).
     */
    public float recentAbsorptionLow;

    /** @see #recentAbsorptionLow */
    public float recentAbsorptionHigh;

    public float absorptionContainer;
    public float armor;
    public float hunger;
    /** Jumpable-mount hearts that replace hunger while riding. Zero slots means show hunger. */
    public float mountHealth;
    public float mountHealthContainer;
    public float air;
    public float experience;
    public int experienceLevel;
    /** Baked hurt/heal flash state for this frame. */
    public boolean heartFlash;
    /** Same idea as {@link #heartFlash}, but for the golden/absorption hearts specifically -- its own independent toggle track. See HudRenderer#renderBar's absorption call. */
    public boolean absorptionFlash;
    /** The native BBS transform sampled from the Layout keyframe. */
    public final Transform layout = new Transform();
    public final Transform slotsLayout = new Transform();
    public final Transform heartsLayout = new Transform();
    public final Transform foodLayout = new Transform();
    public final Transform expLayout = new Transform();
    /** Ticks since the current Health Flash true segment began. */
    public float healthFlashAge;
    public float alpha;

    /**
     * Draw-order among multiple simultaneous HUD-overlay clip types (subtitle/image/boss
     * bar/hotbar). CML edition's ClipContext has a matching "applied" counter to fill this in
     * meaningfully; vanilla bbs-mod's ClipContext doesn't, so this addon always leaves it at 0 --
     * harmless here since we draw hotbars in our own pass, never interleaved with vanilla's
     * subtitle rendering.
     */
    public int renderOrder;
}
