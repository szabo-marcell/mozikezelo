package hu.unideb.inf.mozikezelo.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * A böngészős bejelentkezés sütijei.
 * <ul>
 *     <li>{@value #TOKEN_SUTI}: maga a JWT. HttpOnly, így JavaScriptből (egy esetleges XSS-ből) nem olvasható ki.</li>
 *     <li>{@value #JELZO_SUTI}: titkot nem tartalmazó jelző, amiből a frontend tudja, hogy be van-e jelentkezve.
 *     Ugyanakkor jár le, mint a token.</li>
 * </ul>
 * Mindkettő SameSite=Strict, tehát más oldalról indított kéréshez a böngésző nem csatolja őket.
 */
@Component
public class AuthSutik {

    public static final String TOKEN_SUTI = "MOZI_TOKEN";
    public static final String JELZO_SUTI = "MOZI_BEJELENTKEZVE";

    private final boolean secure;

    public AuthSutik(@Value("${app.jwt.cookie-secure:true}") boolean secure) {
        this.secure = secure;
    }

    public void beallit(HttpServletResponse response, String token, long lejaratMasodperc) {
        Duration eletkor = Duration.ofSeconds(lejaratMasodperc);
        response.addHeader(HttpHeaders.SET_COOKIE, suti(TOKEN_SUTI, token, true, eletkor));
        response.addHeader(HttpHeaders.SET_COOKIE, suti(JELZO_SUTI, "1", false, eletkor));
    }

    public void torol(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, suti(TOKEN_SUTI, "", true, Duration.ZERO));
        response.addHeader(HttpHeaders.SET_COOKIE, suti(JELZO_SUTI, "", false, Duration.ZERO));
    }

    public Optional<String> token(HttpServletRequest request) {
        Cookie[] sutik = request.getCookies();
        if (sutik == null) {
            return Optional.empty();
        }
        return Arrays.stream(sutik)
                .filter(s -> TOKEN_SUTI.equals(s.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private String suti(String nev, String ertek, boolean httpOnly, Duration eletkor) {
        return ResponseCookie.from(nev, ertek)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(eletkor)
                .build()
                .toString();
    }
}
