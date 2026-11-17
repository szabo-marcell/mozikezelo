package hu.unideb.inf.mozikezelo;

import hu.unideb.inf.mozikezelo.config.AuthSutik;
import hu.unideb.inf.mozikezelo.data.entity.*;
import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import hu.unideb.inf.mozikezelo.data.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MoziApiTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MoziRepository moziRepository;
    @Autowired JogRepository jogRepository;
    @Autowired FelhasznaloRepository felhasznaloRepository;
    @Autowired TeremRepository teremRepository;
    @Autowired JegyRepository jegyRepository;
    @Autowired FilmRepository filmRepository;
    @Autowired NapiBeosztasRepository beosztasRepository;
    @Autowired JegyfoglalasRepository foglalasRepository;
    @Autowired FelbehagyottRendelesRepository felbehagyottRepository;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}") String jwtSecret;

    private final LocalDate holnap = LocalDate.now().plusDays(1);
    private Long beosztasId;
    private Long teremId;

    @BeforeEach
    void adatok() {
        felbehagyottRepository.deleteAll();
        foglalasRepository.deleteAll();
        beosztasRepository.deleteAll();
        filmRepository.deleteAll();
        jegyRepository.deleteAll();
        teremRepository.deleteAll();
        felhasznaloRepository.deleteAll();
        jogRepository.deleteAll();
        moziRepository.deleteAll();

        moziRepository.save(new MoziEntity("Teszt Mozi", "Debrecen", "Fő utca 1.", 2004));
        TeremEntity terem = new TeremEntity();
        terem.setNev("Kamaraterem");
        terem.setEmelet(1);
        terem.setFerohely(3);
        terem.setVetitesTipus(VetitesTipus.KET_D);
        teremId = teremRepository.save(terem).getId();

        JegyEntity jegy = jegyRepository.save(new JegyEntity(JegyTipus.DIAK, 2390));
        jegyRepository.save(new JegyEntity(JegyTipus.TELJES_ARU, 2990));
        jegyRepository.save(new JegyEntity(JegyTipus.NYUGDIJAS, 2190));
        jegyRepository.save(new JegyEntity(JegyTipus.GYEREK, 1990));

        FilmEntity film = new FilmEntity();
        film.setCim("Teszt Film");
        film.setBemutatoDatum(LocalDate.of(2026, 10, 1));
        film.setHosszPerc(100);
        film.setMufaj("dráma");
        film.setGyartoOrszag("Magyarország");
        film.setForgalmazo("F");
        film.setGyartoCeg("G");
        film.setFoszereplo("Sz");
        film.setRendezo("R");
        filmRepository.save(film);

        NapiBeosztasEntity b = new NapiBeosztasEntity();
        b.setFilm(film);
        b.setTerem(terem);
        b.setDatum(holnap);
        b.setKezdes(LocalTime.of(18, 0));
        b.setBefejezes(LocalTime.of(20, 0));
        b.setJegy(jegy);
        beosztasId = beosztasRepository.save(b).getId();
    }

    @Test
    void nyilvanosKeresesek() throws Exception {
        mvc.perform(get("/api/mozi")).andExpect(status().isOk()).andExpect(jsonPath("$.nev").value("Teszt Mozi"));
        mvc.perform(get("/api/filmek").param("cim", "teszt film"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/filmek").param("cim", "Teszt"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/filmek").param("bemutatoDatum", "2026-10-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cim").value("Teszt Film"));
        mvc.perform(get("/api/termek/" + teremId)).andExpect(status().isOk()).andExpect(jsonPath("$.nev").value("Kamaraterem"));
        mvc.perform(get("/api/termek/kereses").param("nev", "Kamaraterem")).andExpect(status().isOk());
        mvc.perform(get("/api/termek/kereses").param("nev", "Nincs ilyen")).andExpect(status().isNotFound());
        mvc.perform(get("/api/napi-beosztas").param("datum", holnap.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filmCim").value("Teszt Film"))
                .andExpect(jsonPath("$[0].foglalasUrl").value("/api/napi-beosztas/" + beosztasId + "/foglalas"));
    }

    private String tokenKorral(String email, int kor) throws Exception {
        LocalDate szul = LocalDate.of(LocalDate.now().getYear() - kor, 12, 31);
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Teszt","email":"%s","jelszo":"titkos123","szuletesiDatum":"%s"}""".formatted(email, szul)))
                .andExpect(status().isCreated());
        return belep(email, "titkos123");
    }

    private String belep(String email, String jelszo) throws Exception {
        MockHttpServletResponse valasz = mvc.perform(post("/auth/bejelentkezes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"jelszo\":\"%s\"}".formatted(email, jelszo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().httpOnly(AuthSutik.TOKEN_SUTI, true))
                .andExpect(cookie().sameSite(AuthSutik.TOKEN_SUTI, "Strict"))
                .andExpect(cookie().httpOnly(AuthSutik.JELZO_SUTI, false))
                .andReturn().getResponse();
        return "Bearer " + valasz.getCookie(AuthSutik.TOKEN_SUTI).getValue();
    }

    private void foglalas(String auth, String jegyNev, int szek, int vartStatusz) throws Exception {
        mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jegyNev\":\"%s\",\"szekSzam\":%d}".formatted(jegyNev, szek)))
                .andExpect(status().is(vartStatusz));
    }

    @Test
    void felbehagyottRendelesEsJegyeim() throws Exception {
        String auth = tokenKorral("jegy@example.com", 20);
        String masik = tokenKorral("masik@example.com", 20);
        String rendeles = """
                {"filmCim":"Teszt Film","datum":"%s","kezdes":"18:00","jegyNev":"%s","szekSzam":%d}""";

        mvc.perform(get("/api/felbehagyott-rendelesek")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/jegyfoglalasok/jegyek")).andExpect(status().isUnauthorized());

        String valasz = mvc.perform(post("/api/felbehagyott-rendelesek").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(rendeles.formatted(holnap, "Diák", 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filmCim").value("Teszt Film"))
                .andExpect(jsonPath("$.folytathato").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(valasz).get("id").asLong();
        mvc.perform(post("/api/felbehagyott-rendelesek").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(rendeles.formatted(holnap, "Teljes árú", 2)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/felbehagyott-rendelesek").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].jegyNev").value("Teljes árú"));

        mvc.perform(post("/api/felbehagyott-rendelesek").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(rendeles.formatted(holnap, "Diák", 9)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/felbehagyott-rendelesek/" + id).header("Authorization", masik))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/felbehagyott-rendelesek/" + id).header("Authorization", masik))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/felbehagyott-rendelesek/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(rendeles.formatted(holnap, "Diák", 3)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.szekSzam").value(3));
        foglalas(masik, "Diák", 3, 201);
        mvc.perform(get("/api/felbehagyott-rendelesek/" + id).header("Authorization", auth))
                .andExpect(jsonPath("$.folytathato").value(false));

        mvc.perform(put("/api/felbehagyott-rendelesek/" + id).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(rendeles.formatted(holnap, "Diák", 1)))
                .andExpect(status().isOk());
        foglalas(auth, "Diák", 1, 201);
        mvc.perform(get("/api/felbehagyott-rendelesek").header("Authorization", auth))
                .andExpect(jsonPath("$", hasSize(0)));

        String jegyek = mvc.perform(get("/api/jegyfoglalasok/jegyek").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].filmCim").value("Teszt Film"))
                .andExpect(jsonPath("$[0].rendezo").value("R"))
                .andExpect(jsonPath("$[0].teremNev").value("Kamaraterem"))
                .andExpect(jsonPath("$[0].szekSzam").value(1))
                .andExpect(jsonPath("$[0].felhasznaloNev").value("Teszt"))
                .andExpect(jsonPath("$[0].ervenyes").value(true))
                .andReturn().getResponse().getContentAsString();
        String kod = objectMapper.readTree(jegyek).get(0).get("foglalasKod").asString();
        mvc.perform(get("/api/jegyfoglalasok/" + kod + "/jegy").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.foglalasKod").value(kod));
        mvc.perform(get("/api/jegyfoglalasok/" + kod + "/jegy").header("Authorization", masik))
                .andExpect(status().isNotFound());
    }

    @Test
    void jegyFeltetelekEllenorzeseSzuletesiEvAlapjan() throws Exception {
        mvc.perform(get("/api/napi-beosztas/" + beosztasId))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus",
                        containsInAnyOrder("GYEREK", "DIAK", "NYUGDIJAS", "TELJES_ARU")));

        String otEves = tokenKorral("ot@example.com", 5);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", otEves))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("GYEREK", "TELJES_ARU")));
        foglalas(otEves, "Diák", 1, 409);

        String hatEves = tokenKorral("hat@example.com", 6);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", hatEves))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("GYEREK", "DIAK", "TELJES_ARU")));

        String gyerek = tokenKorral("gyerek@example.com", 17);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", gyerek))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("GYEREK", "DIAK", "TELJES_ARU")));
        foglalas(gyerek, "Nyugdíjas", 1, 409);
        foglalas(gyerek, "Gyerek", 1, 201);

        String diak = tokenKorral("diak@example.com", 23);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", diak))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("DIAK", "TELJES_ARU")));
        foglalas(diak, "Gyerek", 2, 409);
        foglalas(diak, "Diák", 2, 201);

        String felnott = tokenKorral("felnott@example.com", 24);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", felnott))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("TELJES_ARU")));
        foglalas(felnott, "Diák", 3, 409);
        mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas").header("Authorization", felnott)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jegyNev\":\"Diák\",\"szekSzam\":3}"))
                .andExpect(jsonPath("$.detail", containsString("24 év")));

        String nyugdijas = tokenKorral("nyugdijas@example.com", 65);
        mvc.perform(get("/api/napi-beosztas/" + beosztasId).header("Authorization", nyugdijas))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus", contains("NYUGDIJAS", "TELJES_ARU")));
        foglalas(nyugdijas, "Teljes árú", 3, 201);
    }

    @Test
    void csakAdminVehetFelEsModosithatFilmet() throws Exception {
        JogEntity adminJog = jogRepository.save(new JogEntity(null, "ADMIN", "Mozi adminisztrátor"));
        FelhasznaloEntity admin = new FelhasznaloEntity();
        admin.setNev("Admin");
        admin.setEmail("admin@example.com");
        admin.setJelszo(passwordEncoder.encode("admin1234"));
        admin.setSzuletesiDatum(LocalDate.of(1985, 3, 12));
        admin.setJog(adminJog);
        felhasznaloRepository.save(admin);
        String adminAuth = belep("admin@example.com", "admin1234");
        String felhasznaloAuth = tokenKorral("user@example.com", 30);

        String ujFilm = """
                {"cim":"Új film","bemutatoDatum":"2026-12-01","hosszPerc":%d,"mufaj":"vígjáték",
                 "gyartoOrszag":"Magyarország","forgalmazo":"F","gyartoCeg":"G","foszereplo":"Sz","rendezo":"%s"}""";

        mvc.perform(post("/api/filmek").contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(90, "R")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/filmek").header("Authorization", felhasznaloAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(90, "R")))
                .andExpect(status().isForbidden());
        String valasz = mvc.perform(post("/api/filmek").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(90, "R")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cim").value("Új film"))
                .andReturn().getResponse().getContentAsString();
        long ujId = objectMapper.readTree(valasz).get("id").asLong();
        mvc.perform(post("/api/filmek").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cim\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/filmek/" + ujId).header("Authorization", felhasznaloAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(95, "Másik Rendező")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/filmek/" + ujId).header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(95, "Másik Rendező")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendezo").value("Másik Rendező"))
                .andExpect(jsonPath("$.hosszPerc").value(95));
        mvc.perform(get("/api/filmek/" + ujId)).andExpect(jsonPath("$.rendezo").value("Másik Rendező"));
        mvc.perform(put("/api/filmek/999999").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(95, "R")))
                .andExpect(status().isNotFound());

        Long tesztFilmId = filmRepository.findAll().stream()
                .filter(f -> f.getCim().equals("Teszt Film")).findFirst().orElseThrow().getId();
        mvc.perform(put("/api/filmek/" + tesztFilmId).header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(150, "R")))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/filmek/" + ujId)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/filmek/" + ujId).header("Authorization", felhasznaloAuth))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/filmek/" + tesztFilmId).header("Authorization", adminAuth))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/filmek/" + ujId).header("Authorization", adminAuth))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/filmek/" + ujId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/filmek/" + ujId).header("Authorization", adminAuth))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/termek/" + teremId).header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private String vetites(Long filmId, Long teremId, LocalDate datum, String kezdes) {
        return """
                {"filmId":%d,"teremId":%d,"datum":"%s","kezdes":"%s","eredetiNyelvu":false}"""
                .formatted(filmId, teremId, datum, kezdes);
    }

    @Test
    void adminVetitestVeszFelEsTorolUtkozesNelkul() throws Exception {
        JogEntity adminJog = jogRepository.save(new JogEntity(null, "ADMIN", "Mozi adminisztrátor"));
        FelhasznaloEntity admin = new FelhasznaloEntity();
        admin.setNev("Admin");
        admin.setEmail("admin@example.com");
        admin.setJelszo(passwordEncoder.encode("admin1234"));
        admin.setSzuletesiDatum(LocalDate.of(1985, 3, 12));
        admin.setJog(adminJog);
        felhasznaloRepository.save(admin);
        String adminAuth = belep("admin@example.com", "admin1234");
        String felhasznaloAuth = tokenKorral("user@example.com", 21);

        TeremEntity masikTerem = new TeremEntity();
        masikTerem.setNev("Nagyterem");
        masikTerem.setEmelet(0);
        masikTerem.setFerohely(100);
        masikTerem.setVetitesTipus(VetitesTipus.HAROM_D);
        Long masikTeremId = teremRepository.save(masikTerem).getId();
        Long filmId = filmRepository.findAll().getFirst().getId();

        mvc.perform(post("/api/napi-beosztas").contentType(MediaType.APPLICATION_JSON)
                        .content(vetites(filmId, teremId, holnap, "10:00")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/napi-beosztas").header("Authorization", felhasznaloAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, teremId, holnap, "10:00")))
                .andExpect(status().isForbidden());

        String valasz = mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, teremId, holnap, "16:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.befejezes").value("17:55:00"))
                .andExpect(jsonPath("$.jegyNev").value("Teljes árú"))
                .andReturn().getResponse().getContentAsString();
        long ujId = objectMapper.readTree(valasz).get("id").asLong();

        for (String kezdes : new String[]{"17:00", "15:00", "17:50", "19:59"}) {
            mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                            .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, teremId, holnap, kezdes)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(containsString("Időpont-ütközés")));
        }
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, teremId, holnap, "20:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.befejezes").value("21:55:00"));
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, masikTeremId, holnap, "18:30")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, masikTeremId, holnap, "23:00")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vetites(filmId, teremId, LocalDate.now().minusDays(1), "10:00")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(999999L, teremId, holnap, "10:00")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/napi-beosztas").param("datum", holnap.toString()))
                .andExpect(jsonPath("$", hasSize(4)));

        mvc.perform(delete("/api/napi-beosztas/" + ujId).header("Authorization", felhasznaloAuth))
                .andExpect(status().isForbidden());
        foglalas(felhasznaloAuth, "Diák", 1, 201);
        mvc.perform(delete("/api/napi-beosztas/" + beosztasId).header("Authorization", adminAuth))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/napi-beosztas/" + ujId).header("Authorization", adminAuth))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/napi-beosztas/" + ujId).header("Authorization", adminAuth))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/napi-beosztas").header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(vetites(filmId, teremId, holnap, "16:05")))
                .andExpect(status().isCreated());
    }

    @Test
    void csakNegyfeleJegyLetezhet() {
        org.junit.jupiter.api.Assertions.assertEquals(4, JegyTipus.values().length);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jegyRepository.saveAndFlush(new JegyEntity(JegyTipus.DIAK, 1000)));
    }

    @Test
    void regisztracioBelepesProfilFoglalasEsVisszamondas() throws Exception {
        String szul = LocalDate.of(LocalDate.now().getYear() - 21, 1, 1).toString();
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Anna","email":"anna@example.com","jelszo":"titkos123","szuletesiDatum":"%s"}""".formatted(szul)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jogNev").value("FELHASZNALO"))
                .andExpect(jsonPath("$.jelszo").doesNotExist());
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Anna","email":"anna@example.com","jelszo":"titkos123","szuletesiDatum":"%s"}""".formatted(szul)))
                .andExpect(status().isConflict());
        mvc.perform(post("/auth/bejelentkezes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.com\",\"jelszo\":\"rossz-jelszo\"}"))
                .andExpect(status().isUnauthorized());

        String auth = belep("anna@example.com", "titkos123");

        mvc.perform(get("/api/felhasznalo/profil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/felhasznalo/profil").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("anna@example.com"));

        mvc.perform(get("/api/napi-beosztas/" + beosztasId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.szabadSzekek", contains(1, 2, 3)))
                .andExpect(jsonPath("$.foglalhato").value(true));
        mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jegyNev\":\"Diák\",\"szekSzam\":2}"))
                .andExpect(status().isUnauthorized());
        String kod = objectMapper.readTree(mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jegyNev\":\"Diák\",\"szekSzam\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filmCim").value("Teszt Film"))
                .andExpect(jsonPath("$.teremNev").value("Kamaraterem"))
                .andExpect(jsonPath("$.foglalasKod", startsWith("MZ-")))
                .andReturn().getResponse().getContentAsString()).get("foglalasKod").asString();
        mvc.perform(get("/api/napi-beosztas/" + beosztasId))
                .andExpect(jsonPath("$.szabadSzekek", contains(1, 3)));

        String teljesFoglalas = """
                {"filmCim":"Teszt Film","datum":"%s","kezdes":"18:00","jegyNev":"Diák","szekSzam":%d}""";
        mvc.perform(post("/api/jegyfoglalasok").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(teljesFoglalas.formatted(holnap, 2))).andExpect(status().isConflict());
        mvc.perform(post("/api/jegyfoglalasok").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(teljesFoglalas.formatted(holnap, 9))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/jegyfoglalasok").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(teljesFoglalas.formatted(holnap, 3))).andExpect(status().isCreated());

        mvc.perform(get("/api/jegyfoglalasok").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));

        mvc.perform(delete("/api/jegyfoglalasok/" + kod).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/jegyfoglalasok/" + kod).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/jegyfoglalasok/lemondas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(teljesFoglalas.formatted(holnap, 3))).andExpect(status().isNoContent());
        mvc.perform(get("/api/jegyfoglalasok").header("Authorization", auth))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void sutisHitelesitesCsrfVisszamondasEsKijelentkezes() throws Exception {
        String auth = tokenKorral("suti@example.com", 20);
        Cookie suti = new Cookie(AuthSutik.TOKEN_SUTI, auth.substring("Bearer ".length()));
        String foglalasTorzs = "{\"jegyNev\":\"Diák\",\"szekSzam\":1}";

        mvc.perform(get("/api/felhasznalo/profil").cookie(suti))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("suti@example.com"));

        mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas").cookie(suti)
                        .contentType(MediaType.APPLICATION_JSON).content(foglalasTorzs))
                .andExpect(status().isForbidden());

        Cookie xsrf = mvc.perform(get("/auth/csrf")).andExpect(status().isNoContent())
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        String kod = objectMapper.readTree(mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas")
                        .cookie(suti, xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(foglalasTorzs))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("foglalasKod").asString();

        mvc.perform(get("/api/jegyfoglalasok/jegyek").cookie(suti))
                .andExpect(jsonPath("$[0].foglalasKod").value(kod))
                .andExpect(jsonPath("$[0].lemondhato").value(true));
        mvc.perform(delete("/api/jegyfoglalasok/" + kod).cookie(suti))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/jegyfoglalasok/" + kod).cookie(suti, xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/jegyfoglalasok/jegyek").cookie(suti)).andExpect(jsonPath("$", hasSize(0)));

        String hamis = Jwts.builder().subject("suti@example.com").claim("ver", 0)
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        mvc.perform(get("/api/felhasznalo/profil").cookie(new Cookie(AuthSutik.TOKEN_SUTI, hamis)))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge(AuthSutik.TOKEN_SUTI, 0));
        String kibocsatoNelkul = Jwts.builder().subject("suti@example.com").claim("ver", 0)
                .expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret))).compact();
        mvc.perform(get("/api/felhasznalo/profil").header("Authorization", "Bearer " + kibocsatoNelkul))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/kijelentkezes").cookie(suti, xsrf).header("X-XSRF-TOKEN", xsrf.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthSutik.TOKEN_SUTI, 0))
                .andExpect(cookie().maxAge(AuthSutik.JELZO_SUTI, 0));
        mvc.perform(get("/api/felhasznalo/profil").cookie(suti)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/felhasznalo/profil").header("Authorization", auth)).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/felhasznalo/profil").header("Authorization", belep("suti@example.com", "titkos123")))
                .andExpect(status().isOk());
    }
}
