package Glaxium.POV.integration.mixin.bbs;

import Glaxium.POV.integration.access.bbs.FilmsPovAccess;
import java.util.List;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Films;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {Films.class},
   remap = false
)
public interface FilmsPovAccessor extends FilmsPovAccess {
   @Accessor("controllers")
   @Override
   List<BaseFilmController> bbsPov$getControllers();
}
