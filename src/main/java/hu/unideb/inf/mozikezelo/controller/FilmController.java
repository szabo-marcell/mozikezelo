package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.domain.FilmService;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/filmek")
@RequiredArgsConstructor
@Tag(name = "Filmek")
public class FilmController {

    private final FilmService service;


    @GetMapping
    public List<FilmResponse> keres(
            @RequestParam(required = false) @Size(max = 200) String cim,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bemutatoDatum) {
        return service.keres(cim, bemutatoDatum);
    }

    @GetMapping("/{id}")
    public FilmResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    // Új film felvétele (csak ADMIN, lásd SecurityConfig).
    @PostMapping
    public ResponseEntity<FilmResponse> letrehoz(@Valid @RequestBody FilmRequest request) {
        FilmResponse film = service.letrehoz(request);
        return ResponseEntity.created(URI.create("/api/filmek/" + film.id())).body(film);
    }

    // Film adatainak módosítása (csak ADMIN, lásd SecurityConfig).
    @PutMapping("/{id}")
    public FilmResponse modosit(@PathVariable @Positive Long id, @Valid @RequestBody FilmRequest request) {
        return service.modosit(id, request);
    }

    // Film törlése (csak ADMIN, lásd SecurityConfig). Beosztott vagy foglalt film nem törölhető.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void torol(@PathVariable @Positive Long id) {
        service.torol(id);
    }
}
