package Glaxium.POV.config;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.joml.Vector4f;

/**
 * BBS Settings registration for POV category and options.
 */
public final class PovSettings
{
    public static BakeToggleAllValue toggleAll;
    public static ValueBoolean bakeActions;
    public static ValueBoolean bakeCameraShake;
    public static ValueBoolean bakeParticles;
    public static ValueBoolean bakeBossBars;
    public static ValueBoolean bakeScreenEffects;
    public static CursorTextureValue cursorTexture;
    public static CursorCropValue cursorCrop;
    public static ValueFloat cursorDefaultScale;

    private PovSettings()
    {
    }

    public static void register(SettingsBuilder builder)
    {
        builder.category("pov", Icons.LOOKING);
        toggleAll = new BakeToggleAllValue("toggle_all");
        builder.register(toggleAll);
        bakeActions = builder.getBoolean("bake_actions", true);
        bakeCameraShake = builder.getBoolean("bake_camera_shake", true);
        bakeParticles = builder.getBoolean("bake_particles", true);
        bakeBossBars = builder.getBoolean("bake_boss_bars", true);
        bakeScreenEffects = builder.getBoolean("bake_screen_effects", true);
        cursorTexture = new CursorTextureValue("cursor_texture");
        builder.register(cursorTexture);
        cursorCrop = new CursorCropValue("cursor_crop");
        builder.register(cursorCrop);
        cursorDefaultScale = builder.getFloat("cursor_default_scale", 1F, 0.01F, 10F);
    }

    public static void setAllBake(boolean enable)
    {
        if (bakeActions != null) bakeActions.set(enable);
        if (bakeCameraShake != null) bakeCameraShake.set(enable);
        if (bakeParticles != null) bakeParticles.set(enable);
        if (bakeBossBars != null) bakeBossBars.set(enable);
        if (bakeScreenEffects != null) bakeScreenEffects.set(enable);
    }

    public static boolean areAllBakeEnabled()
    {
        return isBakeGui() && isBakeCameraShake() && isBakeParticles() && isBakeBossBars() && isBakeScreenEffects();
    }

    public static boolean isBakeGui()
    {
        return bakeActions != null && bakeActions.get();
    }

    public static boolean isBakeActions()
    {
        return isBakeGui();
    }

    public static boolean isBakeCameraShake()
    {
        return bakeCameraShake != null && bakeCameraShake.get();
    }

    public static boolean isBakeMenu()
    {
        return isBakeGui();
    }

    public static boolean isBakeParticles()
    {
        return bakeParticles != null && bakeParticles.get();
    }

    public static boolean isBakeBossBars()
    {
        return bakeBossBars != null && bakeBossBars.get();
    }

    public static boolean isBakeStatusEffects()
    {
        return isBakeGui();
    }

    public static boolean isBakeToasts()
    {
        return isBakeGui();
    }

    public static boolean isBakeScreenEffects()
    {
        return bakeScreenEffects != null && bakeScreenEffects.get();
    }

    public static boolean isBakeAnyActions()
    {
        return isBakeGui() || isBakeCameraShake() || isBakeParticles() || isBakeBossBars() || isBakeScreenEffects();
    }

    public static float getCursorDefaultScale()
    {
        return cursorDefaultScale == null ? 1F : cursorDefaultScale.get();
    }

    public static void renderCursor(Batcher2D batcher)
    {
        Link link = cursorTexture == null ? null : cursorTexture.get();

        if (link != null)
        {
            Texture texture = BBSModClient.getTextures().getTexture(link);

            if (texture != null && texture.isValid() && texture.width > 0 && texture.height > 0)
            {
                Vector4f crop = cursorCrop == null ? null : cursorCrop.get();
                float left = crop == null ? 0F : clampCrop(crop.x, texture.width);
                float top = crop == null ? 0F : clampCrop(crop.y, texture.height);
                float right = crop == null ? 0F : clampCrop(crop.z, texture.width - left);
                float bottom = crop == null ? 0F : clampCrop(crop.w, texture.height - top);
                float width = texture.width - left - right;
                float height = texture.height - top - bottom;

                if (width > 0F && height > 0F)
                {
                    batcher.texturedBox(texture, 0xffffffff, 0F, 0F, width, height,
                        left, top, texture.width - right, texture.height - bottom);
                }
                return;
            }
        }

        batcher.icon(Icons.CURSOR, 0, 0);
    }

    private static float clampCrop(float value, float maximum)
    {
        return Float.isFinite(value) ? Math.max(0F, Math.min(value, maximum)) : 0F;
    }
}
