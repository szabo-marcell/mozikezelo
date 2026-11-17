package hu.unideb.inf.mozikezelo.service.dto.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FilmRequest(
        @NotBlank @Size(max = 200) String cim,
        @NotNull LocalDate bemutatoDatum,
        @NotNull @Positive @Max(1000) Integer hosszPerc,
        @NotBlank @Size(max = 60) String mufaj,
        @NotBlank @Size(max = 100) String gyartoOrszag,
        @NotBlank @Size(max = 150) String forgalmazo,
        @NotBlank @Size(max = 150) String gyartoCeg,
        @NotBlank @Size(max = 100) String foszereplo,
        @NotBlank @Size(max = 100) String rendezo,
        @Size(max = 100) String foszinkronszinesz,
        @Size(max = 2000) String leiras,
        @Size(max = 300) @Pattern(regexp = "^/[^/].*", message = "a kép útvonala /-rel kezdődjön (pl. /img/filmek/film.svg)")
        String kepUrl,
        @Size(max = 300) @Pattern(regexp = "^/[^/].*", message = "a logó útvonala /-rel kezdődjön (pl. /img/filmek/film.svg)")
        String logoUrl
) {
}
