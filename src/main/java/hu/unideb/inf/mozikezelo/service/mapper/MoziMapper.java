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

    @Mapping(target = "foglalasKod", source = "entity.foglalasKod")
    @Mapping(target = "filmCim", source = "entity.film.cim")
    @Mapping(target = "logoUrl", source = "entity.film.logoUrl")
    @Mapping(target = "kepUrl", source = "entity.film.kepUrl")
    @Mapping(target = "mufaj", source = "entity.film.mufaj")
    @Mapping(target = "hosszPerc", source = "entity.film.hosszPerc")
    @Mapping(target = "rendezo", source = "entity.film.rendezo")
    @Mapping(target = "foszereplo", source = "entity.film.foszereplo")
    @Mapping(target = "foszinkronszinesz", source = "entity.film.foszinkronszinesz")
    @Mapping(target = "gyartoOrszag", source = "entity.film.gyartoOrszag")
    @Mapping(target = "teremNev", source = "entity.terem.nev")
    @Mapping(target = "emelet", source = "entity.terem.emelet")
    @Mapping(target = "vetitesTipus", source = "entity.terem.vetitesTipus")
    @Mapping(target = "eredetiNyelvu", source = "entity.napiBeosztas.eredetiNyelvu")
    @Mapping(target = "datum", source = "entity.napiBeosztas.datum")
    @Mapping(target = "kezdes", source = "entity.napiBeosztas.kezdes")
    @Mapping(target = "befejezes", source = "entity.napiBeosztas.befejezes")
    @Mapping(target = "jegyNev", source = "entity.jegy.nev")
    @Mapping(target = "ar", source = "entity.jegy.ar")
    @Mapping(target = "szekSzam", source = "entity.szekSzam")
    @Mapping(target = "felhasznaloNev", source = "entity.felhasznalo.nev")
    @Mapping(target = "foglalva", source = "entity.letrehozva")
    @Mapping(target = "ervenyes", source = "ervenyes")
    @Mapping(target = "lemondhato", source = "lemondhato")
    BelepoJegyResponse belepoJegy(JegyfoglalasEntity entity, boolean ervenyes, boolean lemondhato);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "napiBeosztasId", source = "entity.napiBeosztas.id")
    @Mapping(target = "filmCim", source = "entity.napiBeosztas.film.cim")
    @Mapping(target = "logoUrl", source = "entity.napiBeosztas.film.logoUrl")
    @Mapping(target = "teremNev", source = "entity.napiBeosztas.terem.nev")
    @Mapping(target = "vetitesTipus", source = "entity.napiBeosztas.terem.vetitesTipus")
    @Mapping(target = "eredetiNyelvu", source = "entity.napiBeosztas.eredetiNyelvu")
    @Mapping(target = "datum", source = "entity.napiBeosztas.datum")
    @Mapping(target = "kezdes", source = "entity.napiBeosztas.kezdes")
    @Mapping(target = "befejezes", source = "entity.napiBeosztas.befejezes")
    @Mapping(target = "jegyNev", source = "entity.jegy.nev")
    @Mapping(target = "ar", source = "entity.jegy.ar")
    @Mapping(target = "szekSzam", source = "entity.szekSzam")
    @Mapping(target = "modositva", source = "entity.modositva")
    @Mapping(target = "folytathato", source = "folytathato")
    FelbehagyottRendelesResponse felbehagyott(FelbehagyottRendelesEntity entity, boolean folytathato);
}
