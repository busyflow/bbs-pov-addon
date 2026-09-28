package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.PovAddon;
import java.io.File;
import java.io.InputStream;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {ModelManager.class},
   remap = false
)
public abstract class ModelManagerPovMixin {
   @Shadow
   public AssetProvider provider;

   @Inject(
      method = {"loadConfig"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void bbsPov$overrideConfig(Link modelLink, CallbackInfoReturnable<MapType> info) {
      if (modelLink != null) {
         Link configLink = modelLink.combine("config.json");
         if (this.provider != null) {
            File externalFile = this.provider.getFile(configLink);
            if (externalFile != null && externalFile.exists()) {
               return;
            }
         }

         String povPath = "assets/bbs_pov/assets/" + configLink.path;

         try {
            InputStream stream = PovAddon.class.getClassLoader().getResourceAsStream(povPath);
            if (stream != null) {
               InputStream s = stream;

               try {
                  String string = IOUtils.readText(s);
                  if (DataToString.fromString(string) instanceof MapType map) {
                     info.setReturnValue(map);
                  }
               } catch (Throwable var11) {
                  if (stream != null) {
                     try {
                        s.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (stream != null) {
                  stream.close();
               }
            }
         } catch (Exception var12) {
            PovAddon.LOGGER.error("Failed to load POV override config for " + modelLink, var12);
         }
      }
   }
}
