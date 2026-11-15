package hu.unideb.inf.mozikezelo.controller;

import hu.unideb.inf.mozikezelo.service.AuthenticationService;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.mozikezelo.service.dto.TokenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Felhasználó - regisztráció és bejelentkezés")
public class AuthenticationController {

    private final AuthenticationService auth;

    @PostMapping("/regisztracio")
    @ResponseStatus(HttpStatus.CREATED)
    public FelhasznaloResponse regisztracio(@Valid @RequestBody FelhasznaloCreateRequest dto) {
        return auth.regisztracio(dto);
    }

    @PostMapping("/bejelentkezes")
    public TokenResponse bejelentkezes(@Valid @RequestBody FelhasznaloLoginRequest dto, HttpServletRequest request) {
        // A közvetlen kliens címe; az X-Forwarded-For fejlécet szándékosan nem használjuk, mert hamisítható.
        return auth.bejelentkezes(dto, request.getRemoteAddr());
    }
}
