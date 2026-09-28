package Glaxium.POV.integration.mixin.bbs;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.LangKey;
import mchorse.bbs_mod.resources.AssetProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {L10n.class},
   remap = false
)
public class L10nPovMixin {
   @Unique
   private static final Map<String, String> BBS_POV$STRINGS = new HashMap<>();

   @Inject(
      method = {"reload(Ljava/lang/String;Lmchorse/bbs_mod/resources/AssetProvider;)V"},
      at = {@At("RETURN")}
   )
   private void bbsPov$onReload(String lang, AssetProvider provider, CallbackInfo info) {
      L10n l10n = (L10n)this;

      for (Entry<String, String> entry : BBS_POV$STRINGS.entrySet()) {
         LangKey key = (LangKey)l10n.getStrings().get(entry.getKey());
         if (key != null) {
            key.content = entry.getValue();
         } else {
            l10n.getStrings().put(entry.getKey(), new LangKey(null, entry.getKey(), entry.getValue()));
         }
      }
   }

   @Inject(
      method = {"getKey(Ljava/lang/String;Ljava/lang/String;)Lmchorse/bbs_mod/l10n/keys/LangKey;"},
      at = {@At("RETURN")}
   )
   private void bbsPov$onGetKey(String key, String defaultContent, CallbackInfoReturnable<LangKey> info) {
      LangKey langKey = (LangKey)info.getReturnValue();
      if (langKey != null) {
         String custom = BBS_POV$STRINGS.get(key);
         if (custom != null) {
            langKey.content = custom;
         }
      }
   }

   static {
      BBS_POV$STRINGS.put("bbs.config.pov.title", "POV");
      BBS_POV$STRINGS.put("bbs.config.pov.tooltip", "Options related to First Person POV");
      BBS_POV$STRINGS.put("bbs.config.pov.toggle_all", "All");
      BBS_POV$STRINGS.put("bbs.config.pov.toggle_all-comment", "Enable or disable all POV baking options at once.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_actions", "Bake GUI Action");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_actions-comment", "Record GUI actions and cursor motion into replay clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_camera_shake", "Bake Camera Shake");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_camera_shake-comment", "Record vanilla hurt-camera motion into Camera Shake action clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_menu", "Bake Menu");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_menu-comment", "Record Game Menu, Death, and Sleep screens into Menu action clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_particles", "Bake Particles");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_particles-comment", "Record on-screen vanilla particles into Particle Effect clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_boss_bars", "Bake Boss Bars");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_boss_bars-comment", "Record the visible vanilla boss bar into a Boss Bars action clip.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_status_effects", "Bake Status Effects");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_status_effects-comment", "Record active status effects into Status Effects action clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_toasts", "Bake Toasts");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_toasts-comment", "Record Advancement, Recipe, Tutorial, and System toasts into Toast clips.");
      BBS_POV$STRINGS.put("bbs.config.pov.bake_screen_effects", "Bake Screen Effects");
      BBS_POV$STRINGS.put(
         "bbs.config.pov.bake_screen_effects-comment", "Record vanilla screen overlays (e.g. portal, freeze, blindness) into Screen Effects action clips."
      );
      BBS_POV$STRINGS.put("bbs.config.pov.camera_shake_in_pov_mode", "Camera Shake in POV Camera Mode");
      BBS_POV$STRINGS.put("bbs.config.pov.camera_shake_in_pov_mode-comment", "Show baked Camera Shake while the film editor is in POV Camera Mode.");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_texture", "Cursor Texture");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_texture-comment", "Image used for the GUI cursor.");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_crop", "Cursor Crop");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_crop-comment", "Visible area of the cursor image.");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_default_scale", "Cursor Scale");
      BBS_POV$STRINGS.put("bbs.config.pov.cursor_default_scale-comment", "Cursor size multiplier.");
      BBS_POV$STRINGS.put("bbs.ui.camera.clips.bbs_pov:pov", "POV");
   }
}
