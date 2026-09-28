package Glaxium.POV.actions.editor;

import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import mchorse.bbs_mod.ui.film.clips.UIClip;

public final class UIPovActionPanels {
   private static boolean registered;

   private UIPovActionPanels() {
   }

   public static synchronized void register() {
      if (!registered) {
         registered = true;
         UIClip.register(GuiPovActionClip.class, UIGuiActionClip::new);
         UIClip.register(MenuPovActionClip.class, UIMenuActionClip::new);
         UIClip.register(CameraShakePovActionClip.class, UICameraShakeActionClip::new);
         UIClip.register(ParticleEffectPovActionClip.class, UIParticleEffectActionClip::new);
         UIClip.register(BossBarPovActionClip.class, UIBossBarActionClip::new);
         UIClip.register(ScreenEffectPovActionClip.class, UIScreenEffectActionClip::new);
         UIClip.register(StatusEffectsPovActionClip.class, UIStatusEffectsActionClip::new);
         UIClip.register(ToastPovActionClip.class, UIToastActionClip::new);
         UIClip.register(ChatPovActionClip.class, UIChatActionClip::new);
         UIClip.register(SemanticHudPovActionClip.class, UISemanticHudActionClip::new);
      }
   }
}
