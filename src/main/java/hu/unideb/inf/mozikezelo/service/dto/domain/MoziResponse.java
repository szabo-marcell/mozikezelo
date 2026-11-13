package hu.unideb.inf.mozikezelo.service.dto.domain;

public record MoziResponse(
        String nev,
        String varos,
        String utca,
        int epitesEve
) {
}
