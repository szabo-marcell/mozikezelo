package hu.unideb.inf.mozikezelo.service.dto.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Foglalás a napi beosztásból kiválasztott előadásra: csak a jegytípus és a szék kell.
public record EloadasFoglalasRequest(
        @NotBlank @Size(max = 100) String jegyNev,
        @NotNull @Positive Integer szekSzam
) {
}
