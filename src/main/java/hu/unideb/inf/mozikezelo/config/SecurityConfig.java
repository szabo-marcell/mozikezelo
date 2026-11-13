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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String ADMIN_JOG = "ADMIN";

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
                        // A mozi, a filmek, a termek és a napi beosztás bejelentkezés nélkül is böngészhető.
                        .requestMatchers(HttpMethod.GET, "/api/mozi", "/api/filmek/**", "/api/termek/**",
                                "/api/napi-beosztas/**")
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
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
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
