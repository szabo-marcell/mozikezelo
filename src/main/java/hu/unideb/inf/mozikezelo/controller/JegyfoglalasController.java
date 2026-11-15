package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.domain.JegyfoglalasService;
import hu.unideb.inf.mozikezelo.service.dto.domain.BelepoJegyResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/jegyfoglalasok")
@RequiredArgsConstructor
@Tag(name = "Jegyfoglalás")
public class JegyfoglalasController {

    private final JegyfoglalasService service;


    @GetMapping
    public Page<JegyfoglalasResponse> sajat(@AuthenticationPrincipal FelhasznaloEntity felhasznalo,
                                            @PageableDefault(size = 20) Pageable page) {
        return service.sajatFoglalasok(felhasznalo.getId(), page);
    }


    // "Jegyeim": a kifizetett foglalásokból kiállított mozijegyek, a film minden adatával.
    @GetMapping("/jegyek")
    public List<BelepoJegyResponse> jegyeim(@AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.jegyeim(felhasznalo.getId());
    }

    // Egy saját jegy a foglalási kód alapján.
    @GetMapping("/{foglalasKod}/jegy")
    public BelepoJegyResponse jegy(@PathVariable @NotBlank @Size(max = 20) String foglalasKod,
                                   @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.jegyKoddal(felhasznalo.getId(), foglalasKod);
    }

    @PostMapping
    public ResponseEntity<JegyfoglalasResponse> foglal(@Valid @RequestBody JegyfoglalasRequest request,
                                                       @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        JegyfoglalasResponse foglalas = service.foglal(felhasznalo.getId(), request);
        return ResponseEntity.created(URI.create("/api/jegyfoglalasok/" + foglalas.foglalasKod())).body(foglalas);
    }


    @DeleteMapping("/{foglalasKod}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lemondKoddal(@PathVariable @NotBlank @Size(max = 20) String foglalasKod,
                             @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        service.lemondKoddal(felhasznalo.getId(), foglalasKod);
    }


    @PostMapping("/lemondas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lemondAdatokkal(@Valid @RequestBody JegyfoglalasRequest request,
                                @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        service.lemondAdatokkal(felhasznalo.getId(), request);
    }
}
