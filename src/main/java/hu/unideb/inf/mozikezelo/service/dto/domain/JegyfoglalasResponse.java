package hu.unideb.inf.mozikezelo.service.dto.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public record JegyfoglalasResponse(
        String foglalasKod,
        Long napiBeosztasId,
        String filmCim,
        String teremNev,
        LocalDate datum,
        LocalTime kezdes,
        LocalTime befejezes,
        String jegyNev,
        int ar,
        int szekSzam
) {
}
