package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.domain.FelhasznaloService;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/felhasznalo")
@RequiredArgsConstructor
@Tag(name = "Felhasználó - saját profil")
public class FelhasznaloController {

    private final FelhasznaloService service;


    @GetMapping("/profil")
    public FelhasznaloResponse profil(@AuthenticationPrincipal FelhasznaloEntity felhasznalo) {
        return service.profil(felhasznalo.getId());
    }
}
