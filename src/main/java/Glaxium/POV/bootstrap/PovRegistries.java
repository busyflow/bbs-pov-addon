package Glaxium.POV.bootstrap;

import Glaxium.POV.actions.editor.UIPovActionPanels;
import Glaxium.POV.camera.clip.PovCameraClip;
import Glaxium.POV.camera.clip.UIPovCameraClip;
import Glaxium.POV.config.UICursorCropSetting;
import Glaxium.POV.hand.editor.UIModelKeyframeFactory;
import Glaxium.POV.hud.editor.UIHotbarIntegerKeyframeFactory;
import Glaxium.POV.hud.editor.UIHotbarItemKeyframeFactory;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public final class PovRegistries {
   private PovRegistries() {
   }

   public static void register() {
      UICursorCropSetting.register();
      BBSMod.getFactoryCameraClips().register(new Link("bbs_pov", "pov"), PovCameraClip.class, new ClipFactoryData(Icons.VISIBLE, 5614335));
      UIClip.register(PovCameraClip.class, UIPovCameraClip::new);
      UIKeyframeFactory.register(KeyframeFactories.ITEM_STACK, UIHotbarItemKeyframeFactory::new);
      UIKeyframeFactory.register(KeyframeFactories.INTEGER, UIHotbarIntegerKeyframeFactory::new);
      UIKeyframeFactory.registerProperty("pov_hand_model", UIModelKeyframeFactory::new);
      UIKeyframeFactory.registerProperty("model", UIModelKeyframeFactory::new);
      UIPovActionPanels.register();
   }
}
