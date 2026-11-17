package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.data.entity.JogEntity;
import hu.unideb.inf.mozikezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.mozikezelo.data.repository.JogRepository;
import hu.unideb.inf.mozikezelo.service.AuthenticationService;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloCreateRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloLoginRequest;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.mozikezelo.service.dto.TokenResponse;
import hu.unideb.inf.mozikezelo.service.exception.FoglaltFelhasznaloAdatException;
import hu.unideb.inf.mozikezelo.service.mapper.FelhasznaloMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    public static final String ALAP_JOG = "FELHASZNALO";

    private final FelhasznaloRepository frepo;
    private final JogRepository jrepo;
    private final FelhasznaloMapper fmapper;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final BejelentkezesVedelem bejelentkezesVedelem;

    @Override
    @Transactional
    public FelhasznaloResponse regisztracio(FelhasznaloCreateRequest dto) {
        String email = normalizalEmail(dto.email());
        if (frepo.existsByEmail(email)) {
            throw new FoglaltFelhasznaloAdatException("email", "Ezzel az e-mail címmel már regisztráltak.");
        }
        JogEntity jog = jrepo.findByNev(ALAP_JOG)
                .orElseGet(() -> jrepo.save(new JogEntity(null, ALAP_JOG, "Regisztrált mozilátogató")));

        FelhasznaloEntity f = new FelhasznaloEntity();
        f.setNev(dto.nev().strip());
        f.setEmail(email);
        f.setJelszo(passwordEncoder.encode(dto.jelszo()));
        f.setSzuletesiDatum(dto.szuletesiDatum());
        f.setJog(jog);
        return fmapper.toResponse(frepo.saveAndFlush(f));
    }

    @Override
    public TokenResponse bejelentkezes(FelhasznaloLoginRequest dto, String ip) {
        String email = normalizalEmail(dto.email());
        String kliensIp = ip == null ? "ismeretlen" : ip;
        bejelentkezesVedelem.ellenoriz(email, kliensIp);
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, dto.jelszo()));
        } catch (AuthenticationException e) {
            bejelentkezesVedelem.sikertelen(email, kliensIp);
            throw e;
        }
        bejelentkezesVedelem.sikeres(email);
        String token = tokenService.generateToken((FelhasznaloEntity) auth.getPrincipal());
        return new TokenResponse(token, "Bearer", tokenService.getLejaratMasodperc());
    }

    @Override
    @Transactional
    public void kijelentkezes(Long felhasznaloId) {
        frepo.tokenVerzioNovelese(felhasznaloId);
    }

    private static String normalizalEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
