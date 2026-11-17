package hu.unideb.inf.mozikezelo.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FelhasznaloCreateRequest(
        @NotBlank @Size(max = 100) String nev,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 100) String jelszo,
        @NotNull @Past LocalDate szuletesiDatum
) {
}
