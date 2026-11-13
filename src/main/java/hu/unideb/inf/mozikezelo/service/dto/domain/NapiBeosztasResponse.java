package hu.unideb.inf.mozikezelo.service.dto.domain;

import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Egy előadás a napi beosztásban.
 * A reszletekUrl-lel választható ki az előadás, a foglalasUrl a "Jegyfoglalás" gomb célja.
 */
public record NapiBeosztasResponse(
        Long id,
        LocalDate datum,
        LocalTime kezdes,
        LocalTime befejezes,
        boolean eredetiNyelvu,
        Long filmId,
        String filmCim,
        int hosszPerc,
        Long teremId,
        String teremNev,
        VetitesTipus vetitesTipus,
        String jegyNev,
        String reszletekUrl,
        String foglalasUrl
) {
}
