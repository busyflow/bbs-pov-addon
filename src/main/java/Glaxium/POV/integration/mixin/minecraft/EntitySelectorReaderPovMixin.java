package Glaxium.POV.integration.mixin.minecraft;

import com.mojang.brigadier.StringReader;
import net.minecraft.command.EntitySelectorReader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntitySelectorReader.class})
public class EntitySelectorReaderPovMixin {
   @Shadow
   @Final
   private StringReader reader;
   @Shadow
   private String playerName;

   @Inject(
      method = {"readRegular"},
      at = {@At("TAIL")}
   )
   private void bbsPov$allowSlashAndActorNames(CallbackInfo ci) {
      if (this.playerName != null && this.reader.canRead() && !Character.isWhitespace(this.reader.peek())) {
         StringBuilder sb = new StringBuilder(this.playerName);

         while (this.reader.canRead() && !Character.isWhitespace(this.reader.peek())) {
            sb.append(this.reader.read());
         }

         this.playerName = sb.toString();
      }
   }
}
