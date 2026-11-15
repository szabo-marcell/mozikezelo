package hu.unideb.inf.mozikezelo.service;

import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.mozikezelo.service.dto.TokenResponse;

public interface AuthenticationService {

    FelhasznaloResponse regisztracio(FelhasznaloCreateRequest dto);

    // Az IP-cím a próbálkozások korlátozásához kell (BejelentkezesVedelem).
    TokenResponse bejelentkezes(FelhasznaloLoginRequest dto, String ip);
}
