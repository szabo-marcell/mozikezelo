package hu.unideb.inf.mozikezelo.service.dto.domain;

import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;

public record JegyResponse(
        Long id,
        JegyTipus tipus,
        String nev,
        int ar,
        String feltetel
) {
}
