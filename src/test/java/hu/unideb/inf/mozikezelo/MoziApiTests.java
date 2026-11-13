package hu.unideb.inf.mozikezelo;

import hu.unideb.inf.mozikezelo.data.entity.*;
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

        JegyEntity jegy = new JegyEntity();
        jegy.setNev("Diák");
        jegy.setAr(2390);
        jegy.setFeltetel("Diákigazolvány");
        jegyRepository.save(jegy);

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

    @Test
    void regisztracioBelepesProfilFoglalasEsVisszamondas() throws Exception {
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Anna","email":"anna@example.com","jelszo":"titkos123","szuletesiDatum":"2000-01-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jogNev").value("FELHASZNALO"))
                .andExpect(jsonPath("$.jelszo").doesNotExist());
        mvc.perform(post("/auth/regisztracio").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nev":"Anna","email":"anna@example.com","jelszo":"titkos123","szuletesiDatum":"2000-01-01"}"""))
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
