package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.domain.JegyService;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jegyek")
@RequiredArgsConstructor
@Tag(name = "Jegytípusok")
public class JegyController {

    private final JegyService service;

    @GetMapping
    public List<JegyResponse> osszes() {
        return service.osszes();
    }
}
