package hu.unideb.inf.mozikezelo.service.dto.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Foglalás vagy visszamondás az összes adat megadásával: film, időpont (nap + kezdés), jegytípus, szék.
 * A terem neve csak akkor kötelező, ha ugyanaz a film ugyanabban az időpontban több teremben is megy.
 */
public record JegyfoglalasRequest(
        @NotBlank @Size(max = 200) String filmCim,
        @NotNull LocalDate datum,
        @NotNull LocalTime kezdes,
        @Size(max = 100) String teremNev,
        @NotBlank @Size(max = 100) String jegyNev,
        @NotNull @Positive Integer szekSzam
) {
}
