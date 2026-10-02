package Glaxium.POV.bootstrap;

/** Registers BBS-POV string files and English fallback keys. */
public final class PovLocalization
{
    private PovLocalization()
    {
    }

    public static void register()
    {
        if (mchorse.bbs_mod.BBSModClient.getL10n() == null)
        {
            return;
        }

        mchorse.bbs_mod.BBSModClient.getL10n().registerOne((lang) ->
            new mchorse.bbs_mod.resources.Link("bbs_pov", "strings/" + lang + ".json"));
        mchorse.bbs_mod.BBSModClient.getL10n().registerOne((lang) ->
            new mchorse.bbs_mod.resources.Link("bbs_pov", "assets/strings/" + lang + ".json"));

        if (mchorse.bbs_mod.BBSModClient.getL10n().getStrings() == null)
        {
            return;
        }

        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.title",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.title", "POV"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.tooltip",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.tooltip", "Options related to First Person POV"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.toggle_all",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.toggle_all", "All"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.toggle_all-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.toggle_all-comment", "Enable or disable all POV baking options at once."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_actions",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_actions", "Bake GUI"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_actions-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_actions-comment", "Record GUI actions, screens, menus, toasts, and status effects into replay clips."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_camera_shake",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_camera_shake", "Bake Camera Shake"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_camera_shake-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_camera_shake-comment", "Record vanilla hurt-camera motion into Camera Shake action clips."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_particles",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_particles", "Bake Particles"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_particles-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_particles-comment", "Record on-screen vanilla particles into Particle Effect clips."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_boss_bars",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_boss_bars", "Bake Boss Bars"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_boss_bars-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_boss_bars-comment", "Record boss bars into Boss Bar action clips."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_screen_effects",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_screen_effects", "Bake Screen Effects"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_screen_effects-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.bake_screen_effects-comment", "Record vanilla screen overlays (e.g. portal, freeze, blindness) into Screen Effects action clips."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_texture",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_texture", "Cursor Texture"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_texture-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_texture-comment", "Image used for the GUI cursor."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_crop",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_crop", "Cursor Crop"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_crop-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_crop-comment", "Visible area of the cursor image."));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_default_scale",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_default_scale", "Cursor Scale"));
        mchorse.bbs_mod.BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_default_scale-comment",
            new mchorse.bbs_mod.l10n.keys.LangKey(null, "bbs.config.pov.cursor_default_scale-comment", "Cursor size multiplier."));
    }
}
