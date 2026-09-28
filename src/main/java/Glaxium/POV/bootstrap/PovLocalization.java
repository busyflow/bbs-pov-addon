package Glaxium.POV.bootstrap;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.l10n.keys.LangKey;
import mchorse.bbs_mod.resources.Link;

public final class PovLocalization {
   private PovLocalization() {
   }

   public static void register() {
      if (BBSModClient.getL10n() != null) {
         BBSModClient.getL10n().registerOne(lang -> new Link("bbs_pov", "strings/" + lang + ".json"));
         BBSModClient.getL10n().registerOne(lang -> new Link("bbs_pov", "assets/strings/" + lang + ".json"));
         if (BBSModClient.getL10n().getStrings() != null) {
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.title", new LangKey(null, "bbs.config.pov.title", "POV"));
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.tooltip", new LangKey(null, "bbs.config.pov.tooltip", "Options related to First Person POV"));
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.toggle_all", new LangKey(null, "bbs.config.pov.toggle_all", "All"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.toggle_all-comment",
                  new LangKey(null, "bbs.config.pov.toggle_all-comment", "Enable or disable all POV baking options at once.")
               );
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_actions", new LangKey(null, "bbs.config.pov.bake_actions", "Bake GUI"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.bake_actions-comment",
                  new LangKey(null, "bbs.config.pov.bake_actions-comment", "Record GUI actions, screens, menus, toasts, and status effects into replay clips.")
               );
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.bake_camera_shake", new LangKey(null, "bbs.config.pov.bake_camera_shake", "Bake Camera Shake"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.bake_camera_shake-comment",
                  new LangKey(null, "bbs.config.pov.bake_camera_shake-comment", "Record vanilla hurt-camera motion into Camera Shake action clips.")
               );
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_particles", new LangKey(null, "bbs.config.pov.bake_particles", "Bake Particles"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.bake_particles-comment",
                  new LangKey(null, "bbs.config.pov.bake_particles-comment", "Record on-screen vanilla particles into Particle Effect clips.")
               );
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.bake_boss_bars", new LangKey(null, "bbs.config.pov.bake_boss_bars", "Bake Boss Bars"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.bake_boss_bars-comment",
                  new LangKey(null, "bbs.config.pov.bake_boss_bars-comment", "Record boss bars into Boss Bar action clips.")
               );
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.bake_screen_effects", new LangKey(null, "bbs.config.pov.bake_screen_effects", "Bake Screen Effects"));
            BBSModClient.getL10n()
               .getStrings()
               .put(
                  "bbs.config.pov.bake_screen_effects-comment",
                  new LangKey(
                     null,
                     "bbs.config.pov.bake_screen_effects-comment",
                     "Record vanilla screen overlays (e.g. portal, freeze, blindness) into Screen Effects action clips."
                  )
               );
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_texture", new LangKey(null, "bbs.config.pov.cursor_texture", "Cursor Texture"));
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.cursor_texture-comment", new LangKey(null, "bbs.config.pov.cursor_texture-comment", "Image used for the GUI cursor."));
            BBSModClient.getL10n().getStrings().put("bbs.config.pov.cursor_crop", new LangKey(null, "bbs.config.pov.cursor_crop", "Cursor Crop"));
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.cursor_crop-comment", new LangKey(null, "bbs.config.pov.cursor_crop-comment", "Visible area of the cursor image."));
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.cursor_default_scale", new LangKey(null, "bbs.config.pov.cursor_default_scale", "Cursor Scale"));
            BBSModClient.getL10n()
               .getStrings()
               .put("bbs.config.pov.cursor_default_scale-comment", new LangKey(null, "bbs.config.pov.cursor_default_scale-comment", "Cursor size multiplier."));
         }
      }
   }
}
