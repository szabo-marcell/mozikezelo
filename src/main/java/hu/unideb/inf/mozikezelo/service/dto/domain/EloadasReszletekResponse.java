package hu.unideb.inf.mozikezelo.service.dto.domain;

import java.util.List;

/**
 * A napi beosztásból kiválasztott előadás: a film, az időpont és a terem már adott,
 * a foglaláshoz csak a jegytípust és a széket kell kiválasztani.
 */
public record EloadasReszletekResponse(
        NapiBeosztasResponse eloadas,
        int ferohely,
        List<Integer> szabadSzekek,
        List<JegyResponse> valaszthatoJegyek,
        boolean foglalhato,
        String foglalasUrl
) {
}
