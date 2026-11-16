package hu.unideb.inf.mozikezelo.service.dto;

/**
 * A bejelentkezés válasza. Maga a token a MOZI_TOKEN HttpOnly sütiben van, nem a törzsben.
 */
public record BejelentkezesResponse(
        long lejaratMasodperc
) {
}
