package hu.unideb.inf.mozikezelo.service.dto.domain;

import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Félbehagyott rendelés. A folytathato hamis, ha az előadás már elkezdődött vagy a széket
 * közben más lefoglalta; ilyenkor a rendelés csak törölhető.
 */
public record FelbehagyottRendelesResponse(
        Long id,
        Long napiBeosztasId,
        String filmCim,
        String logoUrl,
        String teremNev,
        VetitesTipus vetitesTipus,
        boolean eredetiNyelvu,
        LocalDate datum,
        LocalTime kezdes,
        LocalTime befejezes,
        String jegyNev,
        int ar,
        int szekSzam,
        Instant modositva,
        boolean folytathato
) {
}
