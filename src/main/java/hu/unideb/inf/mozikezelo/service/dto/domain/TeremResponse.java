package hu.unideb.inf.mozikezelo.service.dto.domain;

import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;

public record TeremResponse(
        Long id,
        String nev,
        int emelet,
        int ferohely,
        VetitesTipus vetitesTipus
) {
}
