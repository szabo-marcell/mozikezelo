package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.JegyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;

/**
 * A jegytípusok igénylési feltételeinek ellenőrzése a születési dátum alapján.
 * Csak az évet vesszük figyelembe: kor = aktuális év - születési év.
 */
@Component
@RequiredArgsConstructor
public class JegyFeltetelEllenorzo {
    private final Clock clock;

    public int kor(LocalDate szuletesiDatum) {
        return Year.now(clock).getValue() - szuletesiDatum.getYear();
    }

    public boolean igenyelheto(JegyEntity jegy, LocalDate szuletesiDatum) {
        return jegy.getTipus().igenyelheto(kor(szuletesiDatum));
    }
}
