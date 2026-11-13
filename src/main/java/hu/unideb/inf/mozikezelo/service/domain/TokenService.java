package hu.unideb.inf.mozikezelo.service.domain;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class TokenService {

    private final SecretKey secretKey;
    private final long lejaratPerc;

    public TokenService(@Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.lejarat-perc:60}") long lejaratPerc) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.lejaratPerc = lejaratPerc;
    }

    public long getLejaratMasodperc() {
        return lejaratPerc * 60;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public String generateToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        // Csak a jog nevét tesszük a tokenbe, nem az entitást.
        claims.put("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        long most = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date(most))
                .expiration(new Date(most + getLejaratMasodperc() * 1000))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername()) && !isTokenExpired(token);
    }
}
