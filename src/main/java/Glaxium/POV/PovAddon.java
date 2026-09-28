package Glaxium.POV;

import Glaxium.POV.bootstrap.PovLocalization;
import Glaxium.POV.bootstrap.PovRegistries;
import Glaxium.POV.render.PovBlockOutlineRenderer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PovAddon implements ClientModInitializer {
   public static final String MOD_ID = "bbs_pov";
   public static final Logger LOGGER = LoggerFactory.getLogger("bbs_pov");

   public void onInitializeClient() {
      PovRegistries.register();
      PovLocalization.register();
      PovBlockOutlineRenderer.init();
      LOGGER.info("Enabled POV Editor for BBS 2.5");
   }
}
