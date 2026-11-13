package hu.unideb.inf.mozikezelo.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A bejelentkezési azonosító az e-mail cím.
public record FelhasznaloLoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String jelszo
) {
}
