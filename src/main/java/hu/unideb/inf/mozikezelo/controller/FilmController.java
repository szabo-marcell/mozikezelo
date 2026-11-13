package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.domain.FilmService;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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
}
