package hu.unideb.inf.mozikezelo.service.mapper;

import hu.unideb.inf.mozikezelo.data.entity.*;
import hu.unideb.inf.mozikezelo.service.dto.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MoziMapper {

    String BEOSZTAS_URL = "/api/napi-beosztas/";

    MoziResponse mozi(MoziEntity entity);

    FilmResponse film(FilmEntity entity);

    @Mapping(target = "id", ignore = true)
    FilmEntity ujFilm(FilmRequest request);

    @Mapping(target = "id", ignore = true)
    void filmFrissites(FilmRequest request, @MappingTarget FilmEntity entity);

    TeremResponse terem(TeremEntity entity);

    JegyResponse jegy(JegyEntity entity);

    @Mapping(target = "filmId", source = "film.id")
    @Mapping(target = "filmCim", source = "film.cim")
    @Mapping(target = "hosszPerc", source = "film.hosszPerc")
    @Mapping(target = "teremId", source = "terem.id")
    @Mapping(target = "teremNev", source = "terem.nev")
    @Mapping(target = "vetitesTipus", source = "terem.vetitesTipus")
    @Mapping(target = "jegyNev", source = "jegy.nev")
    @Mapping(target = "reszletekUrl", expression = "java(MoziMapper.BEOSZTAS_URL + entity.getId())")
    @Mapping(target = "foglalasUrl", expression = "java(MoziMapper.BEOSZTAS_URL + entity.getId() + \"/foglalas\")")
    NapiBeosztasResponse beosztas(NapiBeosztasEntity entity);

    @Mapping(target = "napiBeosztasId", source = "napiBeosztas.id")
    @Mapping(target = "filmCim", source = "film.cim")
    @Mapping(target = "teremNev", source = "terem.nev")
    @Mapping(target = "datum", source = "napiBeosztas.datum")
    @Mapping(target = "kezdes", source = "napiBeosztas.kezdes")
    @Mapping(target = "befejezes", source = "napiBeosztas.befejezes")
    @Mapping(target = "jegyNev", source = "jegy.nev")
    @Mapping(target = "ar", source = "jegy.ar")
    JegyfoglalasResponse foglalas(JegyfoglalasEntity entity);
}
