package hu.unideb.inf.mozikezelo.config;

import hu.unideb.inf.mozikezelo.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ADMIN_JOG = "ADMIN";

    /**
     * A frontend minden szkriptje és stílusa külön fájlban van (js/, css/), ezért az inline kód tiltható:
     * egy esetleges XSS-hiba esetén a befecskendezett szkript nem fut le, és nem tud adatot kiküldeni.
     */
    static final String SZIGORU_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; "
            + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

    static final String ESZKOZ_CSP = "default-src 'self'; script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'self'";

    private static final RequestMatcher SAJAT_CSP_NELKULI_UTAK = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/h2/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"));

    private final UserService userService;
    private final JwtFilter jwtFilter;

    public SecurityConfig(UserService userService, JwtFilter jwtFilter) {
        this.userService = userService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Regisztráció, bejelentkezés, H2-konzol, API-dokumentáció.
                        .requestMatchers("/auth/**", "/h2/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/error")
                        .permitAll()
                        // A frontend statikus oldalai (HTML, CSS, JS).
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/*.html", "/css/**", "/js/**", "/img/**",
                                "/favicon.ico")
                        .permitAll()
                        // A mozi, a filmek, a termek, a jegytípusok és a napi beosztás bejelentkezés nélkül is böngészhető.
                        .requestMatchers(HttpMethod.GET, "/api/mozi", "/api/filmek/**", "/api/termek/**",
                                "/api/napi-beosztas/**", "/api/jegyek")
                        .permitAll()
                        // Filmet felvenni, módosítani és törölni csak ADMIN jogú felhasználó tud. Mást az ADMIN sem módosíthat.
                        .requestMatchers(HttpMethod.POST, "/api/filmek").hasAuthority(ADMIN_JOG)
                        .requestMatchers(HttpMethod.PUT, "/api/filmek/*").hasAuthority(ADMIN_JOG)
                        .requestMatchers(HttpMethod.DELETE, "/api/filmek/*").hasAuthority(ADMIN_JOG)
                        // Profil, foglalás és visszamondás csak bejelentkezve.
                        .anyRequest()
                        .authenticated())
                .sessionManagement(m -> m.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Token nélküli védett kérésre 401-es válasz.
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authenticationProvider(authProvider())
                .headers(h -> h
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        // Szigorú CSP az alkalmazás oldalaira: csak saját szkript és stílus, inline JS nem fut.
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(SAJAT_CSP_NELKULI_UTAK),
                                new ContentSecurityPolicyHeaderWriter(SZIGORU_CSP)))
                        // A H2-konzol és a Swagger UI inline szkriptet/stílust használ, ezeknek lazább szabály.
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                SAJAT_CSP_NELKULI_UTAK,
                                new ContentSecurityPolicyHeaderWriter(ESZKOZ_CSP)))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService.getUserDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration conf) throws Exception {
        return conf.getAuthenticationManager();
    }
}
