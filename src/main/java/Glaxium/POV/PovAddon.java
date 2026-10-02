package Glaxium.POV;

import Glaxium.POV.bootstrap.PovLocalization;
import Glaxium.POV.bootstrap.PovRegistries;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** BBS-POV client entrypoint for BBS 2.5. */
public final class PovAddon implements ClientModInitializer
{
    public static final String MOD_ID = "bbs_pov";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient()
    {
        PovRegistries.register();
        PovLocalization.register();
        Glaxium.POV.render.PovBlockOutlineRenderer.init();
        LOGGER.info("Enabled POV Editor for BBS 2.5");
    }
}
