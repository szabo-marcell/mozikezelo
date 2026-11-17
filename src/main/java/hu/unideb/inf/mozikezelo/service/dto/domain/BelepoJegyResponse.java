package hu.unideb.inf.mozikezelo.service.dto.domain;

import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A kifizetett foglalásból kiállított mozijegy ("Jegyeim"): a film, az előadás, a terem
 * és a foglalás minden adata. Az ervenyes hamis, ha az előadás már véget ért.
 */
public record BelepoJegyResponse(
        String foglalasKod,
        String filmCim,
        String logoUrl,
        String kepUrl,
        String mufaj,
        int hosszPerc,
        String rendezo,
        String foszereplo,
        String foszinkronszinesz,
        String gyartoOrszag,
        String teremNev,
        int emelet,
        VetitesTipus vetitesTipus,
        boolean eredetiNyelvu,
        LocalDate datum,
        LocalTime kezdes,
        LocalTime befejezes,
        String jegyNev,
        int ar,
        int szekSzam,
        String felhasznaloNev,
        Instant foglalva,
        boolean ervenyes,
        boolean lemondhato
) {
}
