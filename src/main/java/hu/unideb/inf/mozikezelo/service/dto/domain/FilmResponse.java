package hu.unideb.inf.mozikezelo.service.dto.domain;

import java.time.LocalDate;

public record FilmResponse(
        Long id,
        String cim,
        LocalDate bemutatoDatum,
        int hosszPerc,
        String mufaj,
        String gyartoOrszag,
        String forgalmazo,
        String gyartoCeg,
        String foszereplo,
        String rendezo,
        String foszinkronszinesz,
        String leiras,
        String kepUrl
) {
}
