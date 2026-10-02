package Glaxium.POV.hud.render;

import Glaxium.POV.hud.HotbarLayoutTransform;
import Glaxium.POV.hud.HudMount;
import Glaxium.POV.hud.HudState;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Random;

/**
 * Ported verbatim (only the package/import above changed) from BBS-CML-EDITION by
 * BBS-POV -- self-contained (only touches Batcher2D/DrawContext/vanilla MC HUD
 * sprites), kept in this addon's own package rather than bbs-mod's. See PovOverlayRenderer for
 * what actually calls this during real Film playback.
 */
public class HudRenderer
{
    private static final int HUD_GREEN = 8453920;
    private static final int BAR_ICON_Y = -17;
    private static final int EXPERIENCE_BAR_Y = -7;
    private static final int EXPERIENCE_TEXT_Y = -13;
    private static final float SCALE_PIVOT_X = 91F;
    private static final float SCALE_PIVOT_Y = 0.5F;
    private static final int MAX_HEALTH_ROWS = 60;
    private static final float MAX_HEALTH_CONTAINER = MAX_HEALTH_ROWS * 10F * 2F;
    private static final Identifier HOTBAR = Identifier.of("minecraft", "hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.of("minecraft", "hud/hotbar_selection");
    private static final Identifier HOTBAR_OFFHAND_LEFT = Identifier.of("minecraft", "hud/hotbar_offhand_left");
    private static final Identifier HEART_CONTAINER = Identifier.of("minecraft", "hud/heart/container");
    private static final Identifier HEART_HARDCORE_CONTAINER = Identifier.of("minecraft", "hud/heart/container_hardcore");
    /**
     * Vanilla's real hurt-flash sprites -- a white-outlined container and a lighter/pinker heart
     * fill, swapped in during the "on" phase of the blink instead of the normal red heart +
     * black-outlined container (confirmed by sampling the actual sprite pixels: container is
     * black (0,0,0) vs container_blinking is white (255,255,255); full is red (255,19,19) vs
     * full_blinking is a lighter pink (255,161,161)). This is what produces the actual white
     * flash effect, as opposed to merely hiding the fill to reveal the pale container underneath.
     */
    private static final Identifier HEART_CONTAINER_BLINKING = Identifier.of("minecraft", "hud/heart/container_blinking");
    private static final Identifier HEART_HARDCORE_CONTAINER_BLINKING = Identifier.of("minecraft", "hud/heart/container_hardcore_blinking");
    private static final Identifier[][] HEART_HALVES = {
            {Identifier.of("minecraft", "hud/heart/half"), Identifier.of("minecraft", "hud/heart/hardcore_half")},
            {Identifier.of("minecraft", "hud/heart/poisoned_half"), Identifier.of("minecraft", "hud/heart/poisoned_hardcore_half")},
            {Identifier.of("minecraft", "hud/heart/withered_half"), Identifier.of("minecraft", "hud/heart/withered_hardcore_half")},
            {Identifier.of("minecraft", "hud/heart/absorbing_half"), Identifier.of("minecraft", "hud/heart/absorbing_hardcore_half")},
            {Identifier.of("minecraft", "hud/heart/frozen_half"), Identifier.of("minecraft", "hud/heart/frozen_hardcore_half")}
    };
    private static final Identifier[][] HEART_FULLS = {
            {Identifier.of("minecraft", "hud/heart/full"), Identifier.of("minecraft", "hud/heart/hardcore_full")},
            {Identifier.of("minecraft", "hud/heart/poisoned_full"), Identifier.of("minecraft", "hud/heart/poisoned_hardcore_full")},
            {Identifier.of("minecraft", "hud/heart/withered_full"), Identifier.of("minecraft", "hud/heart/withered_hardcore_full")},
            {Identifier.of("minecraft", "hud/heart/absorbing_full"), Identifier.of("minecraft", "hud/heart/absorbing_hardcore_full")},
            {Identifier.of("minecraft", "hud/heart/frozen_full"), Identifier.of("minecraft", "hud/heart/frozen_hardcore_full")}
    };
    private static final Identifier[][] HEART_HALVES_BLINKING = {
            {Identifier.of("minecraft", "hud/heart/half_blinking"), Identifier.of("minecraft", "hud/heart/hardcore_half_blinking")},
            {Identifier.of("minecraft", "hud/heart/poisoned_half_blinking"), Identifier.of("minecraft", "hud/heart/poisoned_hardcore_half_blinking")},
            {Identifier.of("minecraft", "hud/heart/withered_half_blinking"), Identifier.of("minecraft", "hud/heart/withered_hardcore_half_blinking")},
            {Identifier.of("minecraft", "hud/heart/absorbing_half_blinking"), Identifier.of("minecraft", "hud/heart/absorbing_hardcore_half_blinking")},
            {Identifier.of("minecraft", "hud/heart/frozen_half_blinking"), Identifier.of("minecraft", "hud/heart/frozen_hardcore_half_blinking")}
    };
    private static final Identifier[][] HEART_FULLS_BLINKING = {
            {Identifier.of("minecraft", "hud/heart/full_blinking"), Identifier.of("minecraft", "hud/heart/hardcore_full_blinking")},
            {Identifier.of("minecraft", "hud/heart/poisoned_full_blinking"), Identifier.of("minecraft", "hud/heart/poisoned_hardcore_full_blinking")},
            {Identifier.of("minecraft", "hud/heart/withered_full_blinking"), Identifier.of("minecraft", "hud/heart/withered_hardcore_full_blinking")},
            {Identifier.of("minecraft", "hud/heart/absorbing_full_blinking"), Identifier.of("minecraft", "hud/heart/absorbing_hardcore_full_blinking")},
            {Identifier.of("minecraft", "hud/heart/frozen_full_blinking"), Identifier.of("minecraft", "hud/heart/frozen_hardcore_full_blinking")}
    };
    private static final Identifier ARMOR_EMPTY = Identifier.of("minecraft", "hud/armor_empty");
    private static final Identifier ARMOR_FULL = Identifier.of("minecraft", "hud/armor_full");
    private static final Identifier ARMOR_HALF = Identifier.of("minecraft", "hud/armor_half");
    private static final Identifier FOOD_EMPTY = Identifier.of("minecraft", "hud/food_empty");
    private static final Identifier FOOD_FULL = Identifier.of("minecraft", "hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.of("minecraft", "hud/food_half");
    private static final Identifier FOOD_EMPTY_HUNGER = Identifier.of("minecraft", "hud/food_empty_hunger");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.of("minecraft", "hud/food_full_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.of("minecraft", "hud/food_half_hunger");
    private static final Identifier VEHICLE_CONTAINER = Identifier.of("minecraft", "hud/heart/vehicle_container");
    private static final Identifier VEHICLE_FULL = Identifier.of("minecraft", "hud/heart/vehicle_full");
    private static final Identifier VEHICLE_HALF = Identifier.of("minecraft", "hud/heart/vehicle_half");
    private static final Identifier AIR = Identifier.of("minecraft", "hud/air");
    private static final Identifier AIR_BURSTING = Identifier.of("minecraft", "hud/air_bursting");
    private static final Identifier EXPERIENCE_BAR_BACKGROUND_TEXTURE = Identifier.of("minecraft", "textures/gui/sprites/hud/experience_bar_background.png");
    private static final Identifier EXPERIENCE_BAR_PROGRESS_TEXTURE = Identifier.of("minecraft", "textures/gui/sprites/hud/experience_bar_progress.png");
    private static boolean wasHeartRegenerationEnabled;
    private static long heartRegenerationStartTick;

    public static void renderHotbars(MatrixStack stack, Batcher2D batcher, List<HudState> hotbars)
    {
        if (hotbars == null || hotbars.isEmpty())
        {
            return;
        }

        int width = Glaxium.POV.render.PovViewportMetrics.getFilmScaledWidth();
        int height = Glaxium.POV.render.PovViewportMetrics.getFilmScaledHeight();

        renderHotbars(stack, batcher, hotbars, 0, 0, width, height);
    }

    public static void renderHotbars(MatrixStack stack, Batcher2D batcher, List<HudState> hotbars, int originX, int originY, int width, int height)
    {
        if (hotbars == null || hotbars.isEmpty())
        {
            return;
        }

        for (HudState hotbar : hotbars)
        {
            renderHotbar(stack, batcher, hotbar, originX, originY, width, height);
        }
    }

    public static void renderHotbar(MatrixStack stack, Batcher2D batcher, HudState hotbar, int originX, int originY, int width, int height)
    {
        float alpha = MathHelper.clamp(hotbar.alpha, 0F, 1F);

        if (alpha <= 0F)
        {
            return;
        }

        /* BBS renders this GUI-space projection into the export framebuffer. At GUI scale 2,
         * one coordinate below already becomes two output pixels, just like vanilla's HUD.
         * Multiplying by the window scale again would therefore make the export twice too big. */
        Transform transform = hotbar.layout;
        float scaleX = safeScale(transform.scale.x);
        float scaleY = safeScale(transform.scale.y);
        int hotbarWidth = 182;
        float x = originX + width / 2F
            + transform.translate.x * HotbarLayoutTransform.PIXELS_PER_UNIT
            - hotbarWidth / 2F;
        /* Transform space uses the conventional positive-up Y axis, while GUI space
         * grows downward, so subtract Y when crossing into screen coordinates. */
        float bottom = originY + height
            - transform.translate.y * HotbarLayoutTransform.PIXELS_PER_UNIT;
        float y = bottom - SCALE_PIVOT_Y - (22F - SCALE_PIVOT_Y) * scaleY;
        batcher.flush();
        stack.push();
        stack.translate(x, y, 0F);
        stack.translate(SCALE_PIVOT_X, SCALE_PIVOT_Y, 0F);
        if (transform.rotate.z != 0F)
        {
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(transform.rotate.z));
        }
        stack.scale(scaleX, scaleY, 1F);
        stack.translate(-SCALE_PIVOT_X, -SCALE_PIVOT_Y, 0F);

        /* HUD layers must ignore world depth to avoid bottom clipping against terrain. */
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        batcher.getContext().setShaderColor(1F, 1F, 1F, alpha);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);

        // Slots transform block (hotbar container, offhand slot, selection, and item stacks)
        stack.push();
        applyLayoutTransform(stack, hotbar.slotsLayout, 91F, 11F);

        drawGuiSprite(batcher.getContext(), HOTBAR, 0, 0, 182, 22);

        boolean hasOffhandItem = hotbar.offhandItem != null && !hotbar.offhandItem.isEmpty();

        if (hasOffhandItem)
        {
            drawGuiSprite(batcher.getContext(), HOTBAR_OFFHAND_LEFT, -29, -1, 29, 24);
        }

        int selectedSlot = MathHelper.clamp(hotbar.selectedSlot, 0, 8);
        drawGuiSprite(batcher.getContext(), HOTBAR_SELECTION, selectedSlot * 20 - 1, -1, 24, 23);

        /* Item glint (enchants) requires depth test in GUI item renderer. */
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1.0F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1.0F).normalize();
        RenderSystem.setupGui3DDiffuseLighting(light0, light1);

        for (int i = 0; i < 9; i++)
        {
            ItemStack stackItem = hotbar.items[i];

            if (stackItem == null || stackItem.isEmpty())
            {
                continue;
            }

            int itemX = 3 + i * 20;
            int itemY = 3;

            batcher.getContext().drawItem(stackItem, itemX, itemY);
            batcher.getContext().drawItemInSlot(batcher.getFont().getRenderer(), stackItem, itemX, itemY);
        }

        if (hasOffhandItem)
        {
            int offhandX = -26;
            int offhandY = 3;

            batcher.getContext().drawItem(hotbar.offhandItem, offhandX, offhandY);
            batcher.getContext().drawItemInSlot(batcher.getFont().getRenderer(), hotbar.offhandItem, offhandX, offhandY);
        }

        batcher.getContext().draw();

        DiffuseLighting.disableGuiDepthLighting();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        batcher.getContext().setShaderColor(1F, 1F, 1F, alpha);
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);

        stack.pop();

        if (hotbar.statusBarsVisible)
        {
            int barsY = BAR_ICON_Y;
            int heartType = MathHelper.clamp(hotbar.heartType, HudState.HEART_NORMAL, HudState.HEART_FROZEN);
            int hardcore = hotbar.hardcore ? 1 : 0;
            Identifier container = hotbar.hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
            Identifier heartHalf = HEART_HALVES[heartType][hardcore];
            Identifier heartFull = HEART_FULLS[heartType][hardcore];
            Identifier containerBlinking = hotbar.hardcore ? HEART_HARDCORE_CONTAINER_BLINKING : HEART_CONTAINER_BLINKING;
            Identifier heartHalfBlinking = HEART_HALVES_BLINKING[heartType][hardcore];
            Identifier heartFullBlinking = HEART_FULLS_BLINKING[heartType][hardcore];
            /* Vanilla's only status-effect exception for absorption is Wither: withered
             * absorption uses the same dark hearts as regular health. Poison and freezing
             * leave absorption golden. */
            int absorptionType = heartType == HudState.HEART_WITHERED
                ? HudState.HEART_WITHERED
                : HudState.HEART_ABSORBING;
            Identifier absorptionHalf = HEART_HALVES[absorptionType][hardcore];
            Identifier absorptionFull = HEART_FULLS[absorptionType][hardcore];
            Identifier absorptionHalfBlinking = HEART_HALVES_BLINKING[absorptionType][hardcore];
            Identifier absorptionFullBlinking = HEART_FULLS_BLINKING[absorptionType][hardcore];
            int healthSlots = MathHelper.ceil(MathHelper.clamp(hotbar.healthContainer, 0F, MAX_HEALTH_CONTAINER) / 2F);
            healthSlots = MathHelper.clamp(healthSlots, 0, MAX_HEALTH_ROWS * 10);
            int healthRows = Math.max(1, Math.min(MAX_HEALTH_ROWS, (healthSlots + 9) / 10));
            int absorptionSlots = MathHelper.ceil(MathHelper.clamp(hotbar.absorptionContainer, 0F, MAX_HEALTH_CONTAINER) / 2F);
            absorptionSlots = MathHelper.clamp(absorptionSlots, 0, MAX_HEALTH_ROWS * 10);
            int absorptionRows = absorptionSlots <= 0 ? 0 : Math.max(1, Math.min(MAX_HEALTH_ROWS, (absorptionSlots + 9) / 10));
            Random heartShakeRandom = hotbar.health <= 4F ? new Random(thisTickSeed()) : null;
            Random hungerShakeRandom = hotbar.hunger <= 6F ? new Random(thisTickSeed() + 17L) : null;
            int regenerationHeartIndex = -1;
            long hudTick = currentHudTick();
            long healthFlashAge = (long) Math.floor(hotbar.healthFlashAge);
            boolean sharedOutlineBlinking = hotbar.heartFlash
                && Math.floorMod(healthFlashAge / 3L, 2L) == 0L;

            if (hotbar.heartRegeneration && healthSlots > 0 && hotbar.health > 0F)
            {
                if (!wasHeartRegenerationEnabled)
                {
                    heartRegenerationStartTick = hudTick;
                }

                wasHeartRegenerationEnabled = true;

                int cycleLength = healthSlots + 5; /* Vanilla-like pacing: one sweep plus idle tail. */
                int cycleIndex = cycleLength <= 0 ? 0 : (int) Math.floorMod(hudTick - heartRegenerationStartTick, cycleLength);

                regenerationHeartIndex = cycleIndex < healthSlots ? cycleIndex : -1;
            }
            else if (wasHeartRegenerationEnabled)
            {
                wasHeartRegenerationEnabled = false;
            }

            // --- Hearts Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.heartsLayout, 40.5F, barsY + 4.5F);

            renderHealthBar(
                batcher,
                hotbar.health,
                hotbar.previousHealth,
                hotbar.heartFlash,
                container,
                containerBlinking,
                heartHalf,
                heartFull,
                heartHalfBlinking,
                heartFullBlinking,
                0,
                barsY,
                healthSlots,
                heartShakeRandom,
                regenerationHeartIndex,
                healthFlashAge);
            if (absorptionSlots > 0)
            {
                renderBar(batcher, hotbar.absorption, hotbar.recentAbsorptionLow, hotbar.recentAbsorptionHigh, hotbar.absorptionFlash, sharedOutlineBlinking, container, absorptionHalf, absorptionFull, containerBlinking, absorptionHalfBlinking, absorptionFullBlinking, 0, barsY - healthRows * 10, absorptionSlots, heartShakeRandom, -1, hudTick);
            }
            if (hotbar.armor > 0F)
            {
                renderBar(batcher, hotbar.armor, ARMOR_EMPTY, ARMOR_HALF, ARMOR_FULL, 0, barsY - (healthRows + absorptionRows) * 10, 10, null, -1);
            }

            stack.pop();

            // --- Food & Mount Health & Air Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.foodLayout, 182F - 40.5F, barsY + 4.5F);

            Identifier foodEmpty = hotbar.hungerEffect ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
            Identifier foodHalf = hotbar.hungerEffect ? FOOD_HALF_HUNGER : FOOD_HALF;
            Identifier foodFull = hotbar.hungerEffect ? FOOD_FULL_HUNGER : FOOD_FULL;
            int mountSlots = MathHelper.ceil(MathHelper.clamp(hotbar.mountHealthContainer, 0F, 60F) / 2F);
            if (mountSlots > 0)
            {
                renderBarReverse(batcher, hotbar.mountHealth, VEHICLE_CONTAINER, VEHICLE_HALF, VEHICLE_FULL, 182 - 9, barsY, mountSlots, null);
            }
            else
            {
                renderBarReverse(batcher, hotbar.hunger, foodEmpty, foodHalf, foodFull, 182 - 9, barsY, 10, hungerShakeRandom);
            }
            renderAirBar(batcher, hotbar.air, 182 - 9, barsY - 10);

            stack.pop();

            // --- XP Bar Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.expLayout, 91F, EXPERIENCE_BAR_Y + 2.5F);

            float experience = MathHelper.clamp(hotbar.experience, 0F, 1F);
            int xpPixels = MathHelper.ceil(experience * 182F);
            batcher.getContext().drawTexture(EXPERIENCE_BAR_BACKGROUND_TEXTURE, 0, EXPERIENCE_BAR_Y, 0F, 0F, 182, 5, 182, 5);
            if (xpPixels > 0)
            {
                batcher.getContext().drawTexture(EXPERIENCE_BAR_PROGRESS_TEXTURE, 0, EXPERIENCE_BAR_Y, 0F, 0F, xpPixels, 5, 182, 5);
            }

            if (hotbar.experienceLevel > 0)
            {
                String level = Integer.toString(hotbar.experienceLevel);
                int levelX = (182 - batcher.getFont().getWidth(level)) / 2;
                int outlineColor = applyAlpha(0x000000, alpha);
                int levelColor = applyAlpha(HUD_GREEN, alpha);

                /* Vanilla-like outlined XP number: no drop shadow, solid contour around glyphs. */
                batcher.text(level, levelX - 1, EXPERIENCE_TEXT_Y, outlineColor, false);
                batcher.text(level, levelX + 1, EXPERIENCE_TEXT_Y, outlineColor, false);
                batcher.text(level, levelX, EXPERIENCE_TEXT_Y - 1, outlineColor, false);
                batcher.text(level, levelX, EXPERIENCE_TEXT_Y + 1, outlineColor, false);
                batcher.text(level, levelX, EXPERIENCE_TEXT_Y, levelColor, false);
            }

            stack.pop();
        }

        batcher.getContext().setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        stack.pop();
        batcher.flush();
    }

    private static void applyLayoutTransform(MatrixStack stack, Transform t, float pivotX, float pivotY)
    {
        if (t == null)
        {
            return;
        }

        float tx = t.translate.x * HotbarLayoutTransform.PIXELS_PER_UNIT;
        float ty = -t.translate.y * HotbarLayoutTransform.PIXELS_PER_UNIT;
        float sx = safeScale(t.scale.x);
        float sy = safeScale(t.scale.y);
        float rz = t.rotate.z;

        stack.translate(tx, ty, 0F);
        if (sx != 1F || sy != 1F || rz != 0F)
        {
            stack.translate(pivotX, pivotY, 0F);
            if (rz != 0F)
            {
                stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(rz));
            }
            if (sx != 1F || sy != 1F)
            {
                stack.scale(sx, sy, 1F);
            }
            stack.translate(-pivotX, -pivotY, 0F);
        }
    }

    private static float safeScale(float value)
    {
        if (!Float.isFinite(value))
        {
            return 1F;
        }

        if (Math.abs(value) < 0.05F)
        {
            return value < 0F ? -0.05F : 0.05F;
        }

        return value;
    }

    /**
     * Draws vanilla's three independent regular-heart layers:
     * phase-appropriate containers, the pale anchored-health flash, and current health.
     * Current health is last so its red fill remains red while only the recently lost
     * portion alternates between pale pink and the normal empty container.
     */
    private static void renderHealthBar(
        Batcher2D batcher,
        float health,
        float previousHealth,
        boolean healthFlash,
        Identifier container,
        Identifier containerBlinking,
        Identifier half,
        Identifier full,
        Identifier halfBlinking,
        Identifier fullBlinking,
        int x,
        int y,
        int slots,
        Random lowHealthShakeRandom,
        int regenerationHeartIndex,
        long healthFlashAge)
    {
        if (slots <= 0)
        {
            return;
        }

        final int flashTicksPerPhase = 3;
        float current = MathHelper.clamp(health, 0F, slots * 2F) / 2F;
        float previous = MathHelper.clamp(previousHealth, 0F, slots * 2F) / 2F;
        boolean showBlinkingPhase = healthFlash
            && Math.floorMod(healthFlashAge / flashTicksPerPhase, 2L) == 0L;
        /* Healing and absorption loss blink only the shared container outline. The pale
         * previous-health fill exists strictly while regular health has decreased. */
        boolean showPaleLayer = showBlinkingPhase && current < previous;

        for (int i = 0; i < slots; i++)
        {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;

            if (lowHealthShakeRandom != null)
            {
                iconY += lowHealthShakeRandom.nextInt(2);
            }

            if (i == regenerationHeartIndex)
            {
                iconY -= 2;
            }

            /* Minecraft flashes its white container and its pale previous-health fill
             * in the same phase, but they remain independent draw layers. */
            Identifier containerToDraw = showBlinkingPhase ? containerBlinking : container;
            drawGuiSprite(batcher.getContext(), containerToDraw, iconX, iconY, 9, 9);

            if (showPaleLayer)
            {
                drawHeartFill(batcher, previous - i, halfBlinking, fullBlinking, iconX, iconY);
            }

            drawHeartFill(batcher, current - i, half, full, iconX, iconY);
        }
    }

    private static void drawHeartFill(
        Batcher2D batcher,
        float amount,
        Identifier half,
        Identifier full,
        int x,
        int y)
    {
        if (amount >= 1F)
        {
            drawGuiSprite(batcher.getContext(), full, x, y, 9, 9);
        }
        else if (amount >= 0.5F)
        {
            drawGuiSprite(batcher.getContext(), half, x, y, 9, 9);
        }
    }

    /** Backward-compatible entry point for bars that never flash (absorption, armor). */
    private static void renderBar(Batcher2D batcher, float value, Identifier empty, Identifier half, Identifier full, int x, int y, int slots, Random lowHealthShakeRandom, int regenerationHeartIndex)
    {
        renderBar(batcher, value, value, value, false, false, empty, half, full, empty, half, full, x, y, slots, lowHealthShakeRandom, regenerationHeartIndex, 0L);
    }

    /** Existing absorption/legacy flash path; regular health uses renderHealthBar above. */
    private static void renderBar(Batcher2D batcher, float value, float recentHealthLow, float recentHealthHigh, boolean heartFlash, boolean sharedOutlineBlinking, Identifier empty, Identifier half, Identifier full, Identifier emptyBlinking, Identifier halfBlinking, Identifier fullBlinking, int x, int y, int slots, Random lowHealthShakeRandom, int regenerationHeartIndex, long hudTick)
    {
        if (slots <= 0)
        {
            return;
        }

        final int FLASH_TICKS_PER_PHASE = 3;

        float normalized = MathHelper.clamp(value, 0F, slots * 2F) / 2F;
        // Damage is already fully handled by heartFlash alone (the real vanilla hurtTime signal,
        // sustained for its actual duration regardless of anything here) -- it doesn't need any
        // help from this. This is specifically about healing: only flash when health has genuinely
        // trended UP over the last ~10 ticks and is currently sitting at/near the top of that recent
        // range, i.e. it just finished rising -- not merely "there was some spread in the window",
        // which could also be true partway through a decrease, or from small interpolation noise
        // that never actually nets out to a real increase.
        boolean recentlyIncreased = (recentHealthHigh - recentHealthLow) > 0.05F && value >= recentHealthHigh - 0.05F;
        boolean heartAffected = heartFlash || recentlyIncreased;
        boolean flashPhaseOn = ((hudTick / FLASH_TICKS_PER_PHASE) % 2L) == 0L;
        boolean outlineBlinking = sharedOutlineBlinking || (heartAffected && !flashPhaseOn);

        for (int i = 0; i < slots; i++)
        {
            int row = i / 10;
            int col = i % 10;
            int iconX = x + col * 8;
            int iconY = y - row * 10;

            if (lowHealthShakeRandom != null)
            {
                iconY += lowHealthShakeRandom.nextInt(2);
            }

            if (i == regenerationHeartIndex)
            {
                iconY -= 2;
            }

            Identifier emptyToDraw = outlineBlinking ? emptyBlinking : empty;
            // Fill sprites deliberately never use the *Blinking variant -- see comment above.
            Identifier fullToDraw = full;
            Identifier halfToDraw = half;

            drawGuiSprite(batcher.getContext(), emptyToDraw, iconX, iconY, 9, 9);

            float current = normalized - i;

            if (current >= 1F)
            {
                drawGuiSprite(batcher.getContext(), fullToDraw, iconX, iconY, 9, 9);
            }
            else if (current >= 0.5F)
            {
                drawGuiSprite(batcher.getContext(), halfToDraw, iconX, iconY, 9, 9);
            }
        }
    }

    private static long thisTickSeed()
    {
        return currentHudTick() * 312871L;
    }

    private static long currentHudTick()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        return mc.world != null ? mc.world.getTime() : System.currentTimeMillis() / 50L;
    }

    private static void renderBarReverse(Batcher2D batcher, float value, Identifier empty, Identifier half, Identifier full, int x, int y, int slots, Random lowHungerShakeRandom)
    {
        if (slots <= 0)
        {
            return;
        }

        float normalized = MathHelper.clamp(value, 0F, slots * 2F) / 2F;

        for (int i = 0; i < slots; i++)
        {
            int row = i / 10;
            int col = i % 10;
            int iconX = x - col * 8;
            int iconY = y - row * 10;

            if (lowHungerShakeRandom != null)
            {
                iconY += lowHungerShakeRandom.nextInt(2);
            }

            drawGuiSprite(batcher.getContext(), empty, iconX, iconY, 9, 9);

            float current = normalized - i;

            if (current >= 1F)
            {
                drawGuiSprite(batcher.getContext(), full, iconX, iconY, 9, 9);
            }
            else if (current >= 0.5F)
            {
                drawGuiSprite(batcher.getContext(), half, iconX, iconY, 9, 9);
            }
        }
    }

    private static int applyAlpha(int color, float alpha)
    {
        int a = MathHelper.clamp(Math.round(MathHelper.clamp(alpha, 0F, 1F) * 255F), 0, 255);

        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void renderAirBar(Batcher2D batcher, float air, int x, int y)
    {
        if (air >= 300F)
        {
            return;
        }

        int full = MathHelper.ceil((air - 2F) * 10F / 300F);
        int popping = MathHelper.ceil(air * 10F / 300F) - full;

        full = MathHelper.clamp(full, 0, 10);
        popping = MathHelper.clamp(popping, 0, 10 - full);

        for (int i = 0; i < full + popping; i++)
        {
            int iconX = x - i * 8;
            Identifier icon = i < full ? AIR : AIR_BURSTING;

            drawGuiSprite(batcher.getContext(), icon, iconX, y, 9, 9);
        }
    }

    public static void drawGuiSprite(DrawContext context, Identifier texture, int x, int y, int width, int height)
    {
        Sprite sprite = MinecraftClient.getInstance().getGuiAtlasManager().getSprite(texture);
        if (sprite == null)
        {
            return;
        }

        float minU = sprite.getMinU();
        float maxU = sprite.getMaxU();
        float minV = sprite.getMinV();
        float maxV = sprite.getMaxV();
        float uSpan = maxU - minU;
        float vSpan = maxV - minV;

        int spriteW = sprite.getContents() != null ? sprite.getContents().getWidth() : 0;
        int spriteH = sprite.getContents() != null ? sprite.getContents().getHeight() : 0;
        float epsU = spriteW > 0 ? (uSpan / (float) spriteW) * 0.05F : 0F;
        float epsV = spriteH > 0 ? (vSpan / (float) spriteH) * 0.05F : 0F;

        float u1 = minU + epsU;
        float u2 = maxU - epsU;
        float v1 = minV + epsV;
        float v2 = maxV - epsV;

        RenderSystem.setShaderTexture(0, sprite.getAtlasId());
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, (float) x, (float) y, 0F).texture(u1, v1).next();
        bufferBuilder.vertex(matrix, (float) x, (float) (y + height), 0F).texture(u1, v2).next();
        bufferBuilder.vertex(matrix, (float) (x + width), (float) (y + height), 0F).texture(u2, v2).next();
        bufferBuilder.vertex(matrix, (float) (x + width), (float) y, 0F).texture(u2, v1).next();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

}
