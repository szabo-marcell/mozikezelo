package hu.unideb.inf.mozikezelo.config;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.service.UserService;
import hu.unideb.inf.mozikezelo.service.domain.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT alapú hitelesítés. A token a böngészőben HttpOnly sütiben érkezik ({@link AuthSutik});
 * más kliensek (tesztek, REST-kliensek) az "Authorization: Bearer ..." fejlécet is használhatják.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenService tokenService;
    private final UserService userService;
    private final AuthSutik sutik;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        boolean fejlecbol = header != null && header.startsWith(BEARER);
        Optional<String> token = fejlecbol ? Optional.of(header.substring(BEARER.length())) : sutik.token(request);

        if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<FelhasznaloEntity> felhasznalo = hitelesit(token.get(), request, response, !fejlecbol);
            if (felhasznalo.isEmpty() && !fejlecbol) {
                sutik.torol(response);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<FelhasznaloEntity> hitelesit(String token, HttpServletRequest request,
                                                  HttpServletResponse response, boolean sutibol) {
        Optional<Claims> claims = tokenService.ellenoriz(token);
        if (claims.isEmpty() || claims.get().getSubject() == null) {
            return Optional.empty();
        }

        FelhasznaloEntity felhasznalo;
        try {
            UserDetails betoltott = userService.getUserDetailsService().loadUserByUsername(claims.get().getSubject());
            felhasznalo = (FelhasznaloEntity) betoltott;
        } catch (UsernameNotFoundException e) {
            return Optional.empty();
        }
        if (!tokenService.felhasznaloje(claims.get(), felhasznalo)) {
            return Optional.empty();
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(felhasznalo, null, felhasznalo.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        if (sutibol && tokenService.megujitando(claims.get())) {
            sutik.beallit(response, tokenService.generateToken(felhasznalo), tokenService.getLejaratMasodperc());
        }
        return Optional.of(felhasznalo);
    }
}
