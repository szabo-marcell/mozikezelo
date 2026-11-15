package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.domain.FelbehagyottRendelesService;
import hu.unideb.inf.mozikezelo.service.dto.domain.FelbehagyottRendelesResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * "Félbehagyott rendeléseim": a fizetés előtt abbahagyott foglalások (csak bejelentkezve).
 * A rendelés a fizetési lépésnél jön létre, sikeres fizetéskor automatikusan törlődik.
 */
@RestController
@RequestMapping("/api/felbehagyott-rendelesek")
@RequiredArgsConstructor
@Tag(name = "Félbehagyott rendelések")
public class FelbehagyottRendelesController {

    private final FelbehagyottRendelesService service;

    @GetMapping
    public List<FelbehagyottRendelesResponse> sajat(@AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.sajat(felhasznalo.getId());
    }

    @GetMapping("/{id}")
    public FelbehagyottRendelesResponse egy(@PathVariable @Positive Long id,
                                            @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.get(felhasznalo.getId(), id);
    }

    @PostMapping
    public ResponseEntity<FelbehagyottRendelesResponse> ment(@Valid @RequestBody JegyfoglalasRequest request,
                                                             @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        FelbehagyottRendelesResponse rendeles = service.ment(felhasznalo.getId(), request);
        return ResponseEntity.created(URI.create("/api/felbehagyott-rendelesek/" + rendeles.id())).body(rendeles);
    }

    @PutMapping("/{id}")
    public FelbehagyottRendelesResponse modosit(@PathVariable @Positive Long id,
                                                @Valid @RequestBody JegyfoglalasRequest request,
                                                @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.modosit(felhasznalo.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void torol(@PathVariable @Positive Long id, @AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        service.torol(felhasznalo.getId(), id);
    }
}
