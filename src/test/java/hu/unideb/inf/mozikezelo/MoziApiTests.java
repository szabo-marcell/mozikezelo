package hu.unideb.inf.mozikezelo;

import hu.unideb.inf.mozikezelo.data.entity.*;
import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import hu.unideb.inf.mozikezelo.data.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final LocalDate holnap = LocalDate.now().plusDays(1);
    private Long beosztasId;
    private Long teremId;

    @BeforeEach
    void adatok() {
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

    // Regisztrál és bejelentkezik egy felhasználó, akinek a születési év alapján számolt kora "kor" év.
    private String tokenKorral(String email, int kor) throws Exception {
        // December 31-i születésnap: csak az év számít, a hónap/nap nem.
        LocalDate szul = LocalDate.of(LocalDate.now().getYear() - kor, 12, 31);
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Teszt","email":"%s","jelszo":"titkos123","szuletesiDatum":"%s"}""".formatted(email, szul)))
                .andExpect(status().isCreated());
        return "Bearer " + objectMapper.readTree(mvc.perform(post("/auth/bejelentkezes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"jelszo\":\"titkos123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asString();
    }

    private void foglalas(String auth, String jegyNev, int szek, int vartStatusz) throws Exception {
        mvc.perform(post("/api/napi-beosztas/" + beosztasId + "/foglalas").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jegyNev\":\"%s\",\"szekSzam\":%d}".formatted(jegyNev, szek)))
                .andExpect(status().is(vartStatusz));
    }

    @Test
    void jegyFeltetelekEllenorzeseSzuletesiEvAlapjan() throws Exception {
        // Bejelentkezés nélkül mind a négy jegytípus látszik.
        mvc.perform(get("/api/napi-beosztas/" + beosztasId))
                .andExpect(jsonPath("$.valaszthatoJegyek[*].tipus",
                        containsInAnyOrder("GYEREK", "DIAK", "NYUGDIJAS", "TELJES_ARU")));

        // A jegyek ár szerint rendezve: Gyerek, Nyugdíjas, Diák, Teljes árú.
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
        // A Teljes árú jegyet bárki megveheti.
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
        String adminAuth = "Bearer " + objectMapper.readTree(mvc.perform(post("/auth/bejelentkezes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"jelszo\":\"admin1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asString();
        String felhasznaloAuth = tokenKorral("user@example.com", 30);

        String ujFilm = """
                {"cim":"Új film","bemutatoDatum":"2026-12-01","hosszPerc":%d,"mufaj":"vígjáték",
                 "gyartoOrszag":"Magyarország","forgalmazo":"F","gyartoCeg":"G","foszereplo":"Sz","rendezo":"%s"}""";

        // Felvétel: token nélkül 401, sima felhasználóként 403, adminként 201.
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

        // Módosítás: sima felhasználó nem, admin igen.
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

        // Beosztott film hossza nem változhat (a vetítések vége ebből számolódott).
        Long tesztFilmId = filmRepository.findAll().stream()
                .filter(f -> f.getCim().equals("Teszt Film")).findFirst().orElseThrow().getId();
        mvc.perform(put("/api/filmek/" + tesztFilmId).header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content(ujFilm.formatted(150, "R")))
                .andExpect(status().isConflict());

        // Törlés: sima felhasználó nem, admin igen; beosztott film nem törölhető.
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

        // Máshoz az admin sem nyúlhat: termet módosítani nem lehet.
        mvc.perform(put("/api/termek/" + teremId).header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void csakNegyfeleJegyLetezhet() {
        org.junit.jupiter.api.Assertions.assertEquals(4, JegyTipus.values().length);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jegyRepository.saveAndFlush(new JegyEntity(JegyTipus.DIAK, 1000)));
    }

    @Test
    void regisztracioBelepesProfilFoglalasEsVisszamondas() throws Exception {
        // 21 éves (születési év alapján), így Diák jegyet foglalhat.
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

        String token = objectMapper.readTree(mvc.perform(post("/auth/bejelentkezes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"anna@example.com\",\"jelszo\":\"titkos123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asString();
        String auth = "Bearer " + token;

        mvc.perform(get("/api/felhasznalo/profil")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/felhasznalo/profil").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("anna@example.com"));

        // Előadás kiválasztása, majd foglalás onnan: csak jegytípus és szék.
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

        // Ugyanaz a szék nem foglalható újra; nem létező szék hibás kérés.
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

        // Visszamondás rendelésazonosítóval, illetve az összes adattal.
        mvc.perform(delete("/api/jegyfoglalasok/" + kod).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/jegyfoglalasok/" + kod).header("Authorization", auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/jegyfoglalasok/lemondas").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(teljesFoglalas.formatted(holnap, 3))).andExpect(status().isNoContent());
        mvc.perform(get("/api/jegyfoglalasok").header("Authorization", auth))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
