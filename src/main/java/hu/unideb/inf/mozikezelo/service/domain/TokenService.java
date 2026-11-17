package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenService {

    static final String KIBOCSATO = "mozikezelo";
    static final String CELKOZONSEG = "mozikezelo-web";
    static final String VERZIO_CLAIM = "ver";

    private final SecretKey secretKey;
    private final JwtParser parser;
    private final Duration lejarat;
    private final Clock clock;

    public TokenService(@Value("${app.jwt.secret:}") String secret,
                        @Value("${app.jwt.lejarat-perc:60}") long lejaratPerc,
                        Clock clock) {
        this.secretKey = kulcs(secret);
        this.lejarat = Duration.ofMinutes(lejaratPerc);
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(KIBOCSATO)
                .requireAudience(CELKOZONSEG)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    private static SecretKey kulcs(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Nincs megadva a JWT aláírókulcs. Állítsd be az APP_JWT_SECRET környezeti "
                    + "változót (Base64, legalább 256 bit).");
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.strip()));
        } catch (DecodingException | WeakKeyException e) {
            throw new IllegalStateException("Az APP_JWT_SECRET érvénytelen: Base64 kódolású, legalább 256 bites kulcs kell.", e);
        }
    }

    public long getLejaratMasodperc() {
        return lejarat.toSeconds();
    }

    public String generateToken(FelhasznaloEntity felhasznalo) {
        Instant most = clock.instant();
        return Jwts.builder()
                .issuer(KIBOCSATO)
                .audience().add(CELKOZONSEG).and()
                .subject(felhasznalo.getUsername())
                .claim(VERZIO_CLAIM, felhasznalo.getTokenVerzio())
                .issuedAt(Date.from(most))
                .expiration(Date.from(most.plus(lejarat)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Egyetlen feldolgozással ellenőrzi az aláírást, a lejáratot, a kibocsátót és a célközönséget.
     * Hibás, lejárt vagy más kulccsal aláírt tokenre üres Optional.
     */
    public Optional<Claims> ellenoriz(String token) {
        try {
            return Optional.of(parser.parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** A token akkor tartozik a felhasználóhoz, ha az alanya és a verziója is egyezik (nem jelentkezett ki azóta). */
    public boolean felhasznaloje(Claims claims, FelhasznaloEntity felhasznalo) {
        Integer verzio = claims.get(VERZIO_CLAIM, Integer.class);
        return felhasznalo.getUsername().equals(claims.getSubject())
                && verzio != null && verzio == felhasznalo.getTokenVerzio();
    }

    /** Csúszó lejárat: ha az élettartam fele már eltelt, aktív használat mellett új tokent adunk ki. */
    public boolean megujitando(Claims claims) {
        Date kiallitva = claims.getIssuedAt();
        return kiallitva == null || kiallitva.toInstant().plus(lejarat.dividedBy(2)).isBefore(clock.instant());
    }
}
