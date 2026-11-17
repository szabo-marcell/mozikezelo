package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.domain.JegyfoglalasService;
import hu.unideb.inf.mozikezelo.service.domain.NapiBeosztasService;
import hu.unideb.inf.mozikezelo.service.dto.domain.EloadasFoglalasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.EloadasReszletekResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.NapiBeosztasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.NapiBeosztasResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Napi beosztás: 1. nap szerinti lista, 2. egy előadás kiválasztása (szabad székek, jegytípusok),
 * 3. "Jegyfoglalás" gomb: a kiválasztott előadásra már csak jegytípust és széket kell megadni.
 */
@RestController
@RequestMapping("/api/napi-beosztas")
@RequiredArgsConstructor
@Tag(name = "Napi beosztás")
public class NapiBeosztasController {

    private final NapiBeosztasService service;
    private final JegyfoglalasService foglalasService;

    // Keresés nap szerint, pl. /api/napi-beosztas?datum=2026-11-13
    @GetMapping
    public List<NapiBeosztasResponse> napSzerint(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        return service.napra(datum);
    }

    // Új vetítés felvétele (csak ADMIN). A befejezést a szerver számolja; teremütközés esetén 409.
    @PostMapping
    public ResponseEntity<NapiBeosztasResponse> letrehoz(@Valid @RequestBody NapiBeosztasRequest request) {
        NapiBeosztasResponse vetites = service.letrehoz(request);
        return ResponseEntity.created(URI.create(vetites.reszletekUrl())).body(vetites);
    }

    // Vetítés törlése (csak ADMIN). Elkezdődött vagy foglalt jeggyel rendelkező vetítés nem törölhető.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void torol(@PathVariable @Positive Long id) {
        service.torol(id);
    }

    // Egy előadás kiválasztása a listából. Tokennel csak a felhasználó által igényelhető jegyek jelennek meg.
    @GetMapping("/{id}")
    public EloadasReszletekResponse eloadas(@PathVariable @Positive Long id,
                                            @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.reszletek(id, felhasznalo == null ? null : felhasznalo.getSzuletesiDatum());
    }

    // Átlépés a jegyfoglalásba: film, időpont és terem az előadásból adott.
    @PostMapping("/{id}/foglalas")
    public ResponseEntity<JegyfoglalasResponse> foglalas(@PathVariable @Positive Long id,
                                                         @Valid @RequestBody EloadasFoglalasRequest request,
                                                         @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        JegyfoglalasResponse foglalas = foglalasService.foglalEloadasra(felhasznalo.getId(), id, request);
        return ResponseEntity.created(URI.create("/api/jegyfoglalasok/" + foglalas.foglalasKod())).body(foglalas);
    }
}
