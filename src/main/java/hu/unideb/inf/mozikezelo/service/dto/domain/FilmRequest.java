package hu.unideb.inf.mozikezelo.service.dto.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Film felvétele vagy módosítása (csak ADMIN). Módosításkor minden mezőt meg kell adni.
public record FilmRequest(
        @NotBlank @Size(max = 200) String cim,
        @NotNull LocalDate bemutatoDatum,
        @NotNull @Positive @Max(1000) Integer hosszPerc,
        @NotBlank @Size(max = 60) String mufaj,
        @NotBlank @Size(max = 100) String gyartoOrszag,
        @NotBlank @Size(max = 150) String forgalmazo,
        @NotBlank @Size(max = 150) String gyartoCeg,
        @NotBlank @Size(max = 100) String foszereplo,
        @NotBlank @Size(max = 100) String rendezo
) {
}
