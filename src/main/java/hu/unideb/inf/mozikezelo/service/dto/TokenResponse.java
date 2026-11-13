package hu.unideb.inf.mozikezelo.service.dto;


public record TokenResponse(
        String token,
        String tipus,
        long lejaratMasodperc
) {
}
