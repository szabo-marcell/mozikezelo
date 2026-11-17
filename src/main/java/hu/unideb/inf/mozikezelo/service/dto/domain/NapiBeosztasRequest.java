package hu.unideb.inf.mozikezelo.service.dto.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Új vetítés felvétele a napi beosztásba (csak ADMIN).
 * A befejezés időpontját a szerver számolja a film hosszából (+ reklám), ezért nem kell megadni.
 * A jegytípus elhagyható: ekkor a teljes árú jegy kerül az előadásra.
 */
public record NapiBeosztasRequest(
        @NotNull @Positive Long filmId,
        @NotNull @Positive Long teremId,
        @NotNull LocalDate datum,
        @NotNull LocalTime kezdes,
        @NotNull Boolean eredetiNyelvu,
        @Positive Long jegyId
) {
}
