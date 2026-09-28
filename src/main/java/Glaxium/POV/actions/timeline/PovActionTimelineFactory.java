package Glaxium.POV.actions.timeline;

import Glaxium.POV.actions.PovActionType;
import Glaxium.POV.actions.clip.BossBarPovActionClip;
import Glaxium.POV.actions.clip.CameraShakePovActionClip;
import Glaxium.POV.actions.clip.ChatPovActionClip;
import Glaxium.POV.actions.clip.GuiPovActionClip;
import Glaxium.POV.actions.clip.MenuPovActionClip;
import Glaxium.POV.actions.clip.ParticleEffectPovActionClip;
import Glaxium.POV.actions.clip.PovActionClip;
import Glaxium.POV.actions.clip.ScreenEffectPovActionClip;
import Glaxium.POV.actions.clip.SemanticHudPovActionClip;
import Glaxium.POV.actions.clip.StatusEffectsPovActionClip;
import Glaxium.POV.actions.clip.ToastPovActionClip;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.factory.IFactory;

public final class PovActionTimelineFactory implements IFactory<Clip, ClipFactoryData> {
   private final Map<Link, PovActionType> types = new LinkedHashMap<>();
   private final Map<PovActionType, Link> links = new EnumMap<>(PovActionType.class);

   public PovActionTimelineFactory() {
      for (PovActionType type : PovActionType.values()) {
         Link link = new Link("bbs_pov", type.id);
         this.types.put(link, type);
         this.links.put(type, link);
      }

      this.registerFriendlyNames();
   }

   private void registerFriendlyNames() {
      if (BBSModClient.getL10n() != null) {
         for (PovActionType type : PovActionType.values()) {
            Link link = this.links.get(type);
            BBSModClient.getL10n().getKey("bbs.ui.camera.clips." + link, type.title).content = type.title;
         }
      }
   }

   public Link getType(Clip clip) {
      PovActionType type = clip instanceof PovActionClip povClip ? povClip.getActionType() : PovActionType.GUI;
      return this.links.get(type);
   }

   public Clip create(Link link) {
      PovActionType type = this.types.getOrDefault(link, PovActionType.GUI);

      Clip clip = (Clip)(switch (type) {
         case GUI -> new GuiPovActionClip();
         case MENU -> new MenuPovActionClip();
         case CAMERA_SHAKE -> new CameraShakePovActionClip();
         case PARTICLE_EFFECT -> new ParticleEffectPovActionClip();
         case BOSS_BARS -> new BossBarPovActionClip();
         case SCREEN_EFFECT -> new ScreenEffectPovActionClip();
         case STATUS_EFFECTS -> new StatusEffectsPovActionClip();
         case TOASTS -> new ToastPovActionClip();
         case CHAT -> new ChatPovActionClip();
         default -> new SemanticHudPovActionClip(type);
      });
      clip.layer.set(type.seedLayer());
      return clip;
   }

   public ClipFactoryData getData(Clip clip) {
      return this.getData(this.getType(clip));
   }

   public ClipFactoryData getData(Link link) {
      PovActionType type = this.types.getOrDefault(link, PovActionType.GUI);
      Icon icon = Icons.ACTION;
      int color = 5627289;
      if (type == PovActionType.GUI) {
         icon = Icons.LAYOUT;
         color = 5614335;
      } else if (type == PovActionType.MENU) {
         icon = Icons.LAYOUT;
         color = 6737151;
      } else if (type == PovActionType.PARTICLE_EFFECT) {
         icon = Icons.PARTICLE;
         color = 16755251;
      } else if (type == PovActionType.CAMERA_SHAKE) {
         icon = Icons.SPHERE;
         color = 16733559;
      } else if (type == PovActionType.SCREEN_EFFECT) {
         icon = Icons.FADING;
         color = 11163135;
      } else if (type == PovActionType.STATUS_EFFECTS) {
         icon = Icons.HEART;
         color = 15615078;
      } else if (type == PovActionType.TOASTS) {
         icon = Icons.BUBBLE;
         color = 16755268;
      } else if (type == PovActionType.CHAT) {
         icon = Icons.CONSOLE;
         color = 4513194;
      } else if (type == PovActionType.BOSS_BARS) {
         icon = Icons.SKULL;
         color = 13386990;
      }

      return new ClipFactoryData(icon, color);
   }

   public Collection<Link> getKeys() {
      return new ArrayList<>(this.types.keySet());
   }
}
