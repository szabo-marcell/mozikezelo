package hu.unideb.inf.mozikezelo;

import hu.unideb.inf.mozikezelo.service.domain.BejelentkezesVedelem;
import hu.unideb.inf.mozikezelo.service.exception.TulSokProbalkozasException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BejelentkezesVedelemTests {

    @Autowired MockMvc mvc;

    // Minden teszt saját IP-címről és e-mail címekkel dolgozik, hogy a közös Spring-kontextusban ne zavarják egymást.
    private final String egyedi = UUID.randomUUID().toString().substring(0, 8);

    private void regisztral(String email) throws Exception {
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Teszt","email":"%s","jelszo":"titkos123","szuletesiDatum":"1990-01-01"}""".formatted(email)))
                .andExpect(status().isCreated());
    }

    private ResultActions belep(String email, String jelszo, String ip) throws Exception {
        return mvc.perform(post("/auth/bejelentkezes")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"jelszo\":\"%s\"}".formatted(email, jelszo)));
    }

    @Test
    void otRosszJelszoUtanAFiokZarolvaHelyesJelszovalIs() throws Exception {
        String email = "zarolt-" + egyedi + "@example.com";
        String masik = "masik-" + egyedi + "@example.com";
        regisztral(email);
        regisztral(masik);
        String ip = "10.1.0.1";

        for (int i = 0; i < 4; i++) {
            belep(email, "rossz-jelszo", ip).andExpect(status().isUnauthorized());
        }
        belep(email, "rossz-jelszo", ip).andExpect(status().isUnauthorized());
        // Az 5. hiba után a helyes jelszó sem enged be, és nem derül ki, hogy helyes volt-e.
        belep(email, "titkos123", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.detail", containsString("15 perc")));
        // Más IP-címről sem: a zárolás a fiókra vonatkozik.
        belep(email, "titkos123", "10.1.0.2").andExpect(status().isTooManyRequests());
        // Ugyanarról az IP-ről egy másik fiók továbbra is beléphet.
        belep(masik, "titkos123", ip).andExpect(status().isOk());
    }

    @Test
    void nemLetezoFiokotIsUgyanugyZarol() throws Exception {
        String email = "nincs-" + egyedi + "@example.com";
        for (int i = 0; i < 5; i++) {
            belep(email, "barmi-jelszo", "10.2.0.1").andExpect(status().isUnauthorized());
        }
        belep(email, "barmi-jelszo", "10.2.0.1").andExpect(status().isTooManyRequests());
    }

    @Test
    void sikeresBelepesNullazzaAFiokSzamlalojat() throws Exception {
        String email = "nullaz-" + egyedi + "@example.com";
        regisztral(email);
        String ip = "10.3.0.1";
        for (int i = 0; i < 4; i++) {
            belep(email, "rossz-jelszo", ip).andExpect(status().isUnauthorized());
        }
        belep(email, "titkos123", ip).andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            belep(email, "rossz-jelszo", ip).andExpect(status().isUnauthorized());
        }
        belep(email, "titkos123", ip).andExpect(status().isOk());
    }

    @Test
    void sokFiokProbalgatasaUgyanarrolAzIpCimrolZarolja() throws Exception {
        String email = "letezo-" + egyedi + "@example.com";
        regisztral(email);
        String ip = "10.4.0.1";
        // 20 különböző e-mail címmel, mindegyikkel csak egyszer (a fiókonkénti korlát így nem lép életbe).
        for (int i = 0; i < 20; i++) {
            belep("spray" + i + "-" + egyedi + "@example.com", "Jelszo123", ip).andExpect(status().isUnauthorized());
        }
        belep(email, "titkos123", ip).andExpect(status().isTooManyRequests());
        belep(email, "titkos123", "10.4.0.2").andExpect(status().isOk());
    }

    @Test
    void aZarolasLejarAzIdoElteltevel() {
        MozgathatoOra ora = new MozgathatoOra(Instant.parse("2026-11-15T10:00:00Z"));
        BejelentkezesVedelem vedelem = new BejelentkezesVedelem(ora, 3, 100, 15, 15);
        for (int i = 0; i < 3; i++) {
            vedelem.ellenoriz("a@example.com", "1.1.1.1");
            vedelem.sikertelen("a@example.com", "1.1.1.1");
        }
        assertThrows(TulSokProbalkozasException.class, () -> vedelem.ellenoriz("a@example.com", "1.1.1.1"));

        ora.elore(Duration.ofMinutes(14));
        assertThrows(TulSokProbalkozasException.class, () -> vedelem.ellenoriz("a@example.com", "1.1.1.1"));
        ora.elore(Duration.ofMinutes(1));
        assertDoesNotThrow(() -> vedelem.ellenoriz("a@example.com", "1.1.1.1"));

        // Az ablakon kívüli régi hibák nem számítanak: 2 hiba, 16 perc szünet, 2 hiba -> nincs zárolás.
        vedelem.sikertelen("b@example.com", "2.2.2.2");
        vedelem.sikertelen("b@example.com", "2.2.2.2");
        ora.elore(Duration.ofMinutes(16));
        vedelem.sikertelen("b@example.com", "2.2.2.2");
        vedelem.sikertelen("b@example.com", "2.2.2.2");
        assertDoesNotThrow(() -> vedelem.ellenoriz("b@example.com", "2.2.2.2"));
    }

    @Test
    void szigoruCspFejlecAzOldalakonEsAzApin() throws Exception {
        for (String ut : new String[]{"/index.html", "/foglalas.html", "/api/mozi"}) {
            mvc.perform(get(ut))
                    .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self';")))
                    .andExpect(header().string("Content-Security-Policy", not(containsString("unsafe-inline"))))
                    .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
        }
        // A HTML-oldalakon nincs inline szkript, amit a CSP letiltana.
        mvc.perform(get("/foglalas.html"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString("<script>"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString("style="))));
    }

    private static final class MozgathatoOra extends Clock {
        private Instant most;

        MozgathatoOra(Instant most) {
            this.most = most;
        }

        void elore(Duration d) {
            most = most.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return most;
        }
    }
}
