package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.config.AuthSutik;
import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.AuthenticationService;
import hu.unideb.inf.mozikezelo.service.dto.BejelentkezesResponse;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.mozikezelo.service.dto.TokenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Felhasználó - regisztráció és bejelentkezés")
public class AuthenticationController {

    private final AuthenticationService auth;
    private final AuthSutik sutik;

    @PostMapping("/regisztracio")
    @ResponseStatus(HttpStatus.CREATED)
    public FelhasznaloResponse regisztracio(@Valid @RequestBody FelhasznaloCreateRequest dto) {
        return auth.regisztracio(dto);
    }

    /**
     * A JWT HttpOnly sütiben érkezik (a válasz törzsében nincs benne), így a böngészőben futó JavaScript nem fér hozzá.
     */
    @PostMapping("/bejelentkezes")
    public BejelentkezesResponse bejelentkezes(@Valid @RequestBody FelhasznaloLoginRequest dto,
                                               HttpServletRequest request, HttpServletResponse response) {
        TokenResponse token = auth.bejelentkezes(dto, request.getRemoteAddr());
        sutik.beallit(response, token.token(), token.lejaratMasodperc());
        return new BejelentkezesResponse(token.lejaratMasodperc());
    }

    /**
     * Kijelentkezés: a felhasználó minden korábban kiadott tokenje érvénytelenné válik (minden eszközön),
     * és a sütik törlődnek. Lejárt munkamenettel is hívható.
     */
    @PostMapping("/kijelentkezes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kijelentkezes(@AuthenticationPrincipal FelhasznaloEntity felhasznalo, HttpServletResponse response) {
        if (felhasznalo != null) {
            auth.kijelentkezes(felhasznalo.getId());
        }
        sutik.torol(response);
    }

    /** Beállítja az XSRF-TOKEN sütit, ha a frontendnek még nincs (a módosító kérésekhez kell). */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void csrf(CsrfToken token) {
        token.getToken();
    }
}
