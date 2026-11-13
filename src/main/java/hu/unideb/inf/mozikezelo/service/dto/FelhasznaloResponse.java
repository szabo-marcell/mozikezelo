package hu.unideb.inf.mozikezelo.service.dto;

import java.time.LocalDate;

public record FelhasznaloResponse(
        Long id,
        String nev,
        String email,
        LocalDate szuletesiDatum,
        String jogNev
) {
}
