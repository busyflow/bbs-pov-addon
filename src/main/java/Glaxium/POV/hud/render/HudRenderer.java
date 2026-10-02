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
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");
    private static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");

    private static boolean wasHeartRegenerationEnabled;
    private static long heartRegenerationStartTick;

    public static void renderHotbars(MatrixStack stack, Batcher2D batcher, List<HudState> hotbars)
    {
        if (hotbars == null || hotbars.isEmpty())
        {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
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

        batcher.getContext().drawTexture(WIDGETS_TEXTURE, 0, 0, 0, 0, 182, 22);

        boolean hasOffhandItem = hotbar.offhandItem != null && !hotbar.offhandItem.isEmpty();

        if (hasOffhandItem)
        {
            batcher.getContext().drawTexture(WIDGETS_TEXTURE, -29, -1, 24, 22, 29, 24);
        }

        int selectedSlot = MathHelper.clamp(hotbar.selectedSlot, 0, 8);
        batcher.getContext().drawTexture(WIDGETS_TEXTURE, selectedSlot * 20 - 1, -1, 0, 22, 24, 24);

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
            int absorptionType = heartType == HudState.HEART_WITHERED
                ? HudState.HEART_WITHERED
                : HudState.HEART_ABSORBING;
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

            // --- Hearts & Absorption & Armor Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.heartsLayout, 40.5F, barsY + 4.5F);

            renderHealthBar(
                batcher,
                hotbar.health,
                hotbar.previousHealth,
                hotbar.heartFlash,
                heartType,
                hotbar.hardcore,
                0,
                barsY,
                healthSlots,
                heartShakeRandom,
                regenerationHeartIndex,
                healthFlashAge);
            if (absorptionSlots > 0)
            {
                renderAbsorptionBar(batcher, hotbar.absorption, hotbar.recentAbsorptionLow, hotbar.recentAbsorptionHigh, hotbar.absorptionFlash, sharedOutlineBlinking, absorptionType, hotbar.hardcore, 0, barsY - healthRows * 10, absorptionSlots, heartShakeRandom, hudTick);
            }
            if (hotbar.armor > 0F)
            {
                renderArmorBar(batcher, hotbar.armor, 0, barsY - (healthRows + absorptionRows) * 10, 10);
            }

            stack.pop();

            // --- Food & Mount Health & Air Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.foodLayout, 182F - 40.5F, barsY + 4.5F);

            int mountSlots = MathHelper.ceil(MathHelper.clamp(hotbar.mountHealthContainer, 0F, 60F) / 2F);
            if (mountSlots > 0)
            {
                renderMountHealthBar(batcher, hotbar.mountHealth, 182 - 9, barsY, mountSlots, null);
            }
            else
            {
                renderFoodBar(batcher, hotbar.hunger, hotbar.hungerEffect, 182 - 9, barsY, 10, hungerShakeRandom);
            }
            renderAirBar(batcher, hotbar.air, 182 - 9, barsY - 10);

            stack.pop();

            // --- XP Bar Transform Block ---
            stack.push();
            applyLayoutTransform(stack, hotbar.expLayout, 91F, EXPERIENCE_BAR_Y + 2.5F);

            float experience = MathHelper.clamp(hotbar.experience, 0F, 1F);
            int xpPixels = MathHelper.ceil(experience * 182F);
            batcher.getContext().drawTexture(ICONS_TEXTURE, 0, EXPERIENCE_BAR_Y, 0, 64, 182, 5);
            if (xpPixels > 0)
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, 0, EXPERIENCE_BAR_Y, 0, 69, xpPixels, 5);
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
        if (rz != 0F || sx != 1F || sy != 1F)
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

    private static void renderHealthBar(
        Batcher2D batcher,
        float health,
        float previousHealth,
        boolean healthFlash,
        int heartType,
        boolean hardcore,
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

            int containerU = 16 + (showBlinkingPhase ? 9 : 0);
            int containerV = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, containerU, containerV, 9, 9);

            if (showPaleLayer)
            {
                drawHeartFill(batcher, previous - i, heartType, hardcore, true, iconX, iconY);
            }

            drawHeartFill(batcher, current - i, heartType, hardcore, false, iconX, iconY);
        }
    }

    private static void drawHeartFill(
        Batcher2D batcher,
        float amount,
        int heartType,
        boolean hardcore,
        boolean blinking,
        int x,
        int y)
    {
        if (amount <= 0F)
        {
            return;
        }

        boolean half = amount < 1F && amount >= 0.5F;
        if (amount >= 0.5F)
        {
            int textureIndex;
            boolean hasBlinking;
            switch (heartType)
            {
                case HudState.HEART_POISONED -> { textureIndex = 4; hasBlinking = true; }
                case HudState.HEART_WITHERED -> { textureIndex = 6; hasBlinking = true; }
                case HudState.HEART_ABSORBING -> { textureIndex = 8; hasBlinking = false; }
                case HudState.HEART_FROZEN -> { textureIndex = 9; hasBlinking = false; }
                default -> { textureIndex = 2; hasBlinking = true; }
            }
            int u = 16 + (textureIndex * 2 + (blinking && hasBlinking ? 2 : 0) + (half ? 1 : 0)) * 9;
            int v = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, x, y, u, v, 9, 9);
        }
    }

    private static void renderAbsorptionBar(
        Batcher2D batcher,
        float value,
        float recentHealthLow,
        float recentHealthHigh,
        boolean heartFlash,
        boolean sharedOutlineBlinking,
        int absorptionType,
        boolean hardcore,
        int x,
        int y,
        int slots,
        Random lowHealthShakeRandom,
        long hudTick)
    {
        if (slots <= 0)
        {
            return;
        }

        final int FLASH_TICKS_PER_PHASE = 3;

        float normalized = MathHelper.clamp(value, 0F, slots * 2F) / 2F;
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

            int containerU = 16 + (outlineBlinking ? 9 : 0);
            int containerV = hardcore ? 45 : 0;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, containerU, containerV, 9, 9);

            float current = normalized - i;
            drawHeartFill(batcher, current, absorptionType, hardcore, false, iconX, iconY);
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

    private static void renderArmorBar(Batcher2D batcher, float value, int x, int y, int slots)
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
            int iconX = x + col * 8;
            int iconY = y - row * 10;

            float current = normalized - i;
            if (current >= 1F)
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 34, 9, 9, 9);
            }
            else if (current >= 0.5F)
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 25, 9, 9, 9);
            }
            else
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 16, 9, 9, 9);
            }
        }
    }

    private static void renderFoodBar(Batcher2D batcher, float value, boolean hungerEffect, int x, int y, int slots, Random lowHungerShakeRandom)
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

            int emptyU = hungerEffect ? 133 : 16;
            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, emptyU, 27, 9, 9);

            float current = normalized - i;
            if (current >= 1F)
            {
                int fullU = hungerEffect ? 88 : 52;
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, fullU, 27, 9, 9);
            }
            else if (current >= 0.5F)
            {
                int halfU = hungerEffect ? 97 : 61;
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, halfU, 27, 9, 9);
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
            int u = i < full ? 16 : 25;

            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, y, u, 18, 9, 9);
        }
    }

    private static void renderMountHealthBar(Batcher2D batcher, float value, int x, int y, int slots, Random shakeRandom)
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
            if (shakeRandom != null)
            {
                iconY += shakeRandom.nextInt(2);
            }

            batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 52, 9, 9, 9);
            float current = normalized - i;
            if (current >= 1F)
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 88, 9, 9, 9);
            }
            else if (current >= 0.5F)
            {
                batcher.getContext().drawTexture(ICONS_TEXTURE, iconX, iconY, 97, 9, 9, 9);
            }
        }
    }
}
