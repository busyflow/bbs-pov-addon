package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.PovAddon;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.InputStream;

@Mixin(value = ModelManager.class, remap = false)
public abstract class ModelManagerPovMixin
{
    @Shadow public AssetProvider provider;

    @Inject(method = "loadConfig", at = @At("HEAD"), cancellable = true)
    private void bbsPov$overrideConfig(Link modelLink, CallbackInfoReturnable<MapType> info)
    {
        if (modelLink == null)
        {
            return;
        }

        Link configLink = modelLink.combine("config.json");

        /* If the user has saved an external config in config/bbs/assets/ (via Model Editor or manual edit),
         * allow BBS to load that external file so user customizations are always preserved. */
        if (this.provider != null)
        {
            File externalFile = this.provider.getFile(configLink);
            if (externalFile != null && externalFile.exists())
            {
                return;
            }
        }

        /* Check if BBS-POV provides default config assets for this model */
        String povPath = "assets/bbs_pov/assets/" + configLink.path;
        try
        {
            InputStream stream = PovAddon.class.getClassLoader().getResourceAsStream(povPath);
            if (stream != null)
            {
                try (InputStream s = stream)
                {
                    String string = IOUtils.readText(s);
                    BaseType data = DataToString.fromString(string);
                    if (data instanceof MapType map)
                    {
                        info.setReturnValue(map);
                    }
                }
            }
        }
        catch (Exception e)
        {
            PovAddon.LOGGER.error("Failed to load POV override config for " + modelLink, e);
        }
    }
}
