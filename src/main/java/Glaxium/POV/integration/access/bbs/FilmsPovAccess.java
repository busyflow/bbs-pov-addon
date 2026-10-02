package Glaxium.POV.integration.access.bbs;

import mchorse.bbs_mod.film.BaseFilmController;

import java.util.List;

/** Feature-facing accessor. Mixin implements this; do not import mixin types from features. */
public interface FilmsPovAccess
{
    List<BaseFilmController> bbsPov$getControllers();
}
