package hu.unideb.inf.mozikezelo.service.dto.domain;

public record JegyResponse(
        Long id,
        String nev,
        int ar,
        String feltetel
) {
}
