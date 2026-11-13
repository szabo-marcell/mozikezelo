package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.domain.MoziService;
import hu.unideb.inf.mozikezelo.service.dto.domain.MoziResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mozi")
@RequiredArgsConstructor
@Tag(name = "Mozi alapadatai")
public class MoziController {

    private final MoziService service;

    @GetMapping
    public MoziResponse alapadatok() {
        return service.alapadatok();
    }
}
