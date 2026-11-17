package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.domain.TeremService;
import hu.unideb.inf.mozikezelo.service.dto.domain.TeremResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/termek")
@RequiredArgsConstructor
@Tag(name = "Termek")
public class TeremController {

    private final TeremService service;

    @GetMapping
    public List<TeremResponse> osszes() {
        return service.osszes();
    }

    @GetMapping("/{id}")
    public TeremResponse azonositoSzerint(@PathVariable @Positive Long id) {
        return service.azonositoSzerint(id);
    }

    @GetMapping("/kereses")
    public TeremResponse nevSzerint(@RequestParam @NotBlank @Size(max = 100) String nev) {
        return service.nevSzerint(nev);
    }
}
