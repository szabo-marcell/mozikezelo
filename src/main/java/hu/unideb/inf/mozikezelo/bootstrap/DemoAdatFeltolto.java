package hu.unideb.inf.mozikezelo.bootstrap;

import hu.unideb.inf.mozikezelo.data.entity.*;
import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import hu.unideb.inf.mozikezelo.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Üres adatbázis feltöltése bemutató adatokkal: mozi, termek, jegytípusok, filmek,
 * a mai és a következő két nap beosztása, valamint két felhasználó.
 * Demo belépés: demo@mozi.hu / demo1234, admin@mozi.hu / admin1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoAdatFeltolto implements CommandLineRunner {

    private final MoziRepository moziRepository;
    private final JogRepository jogRepository;
    private final FelhasznaloRepository felhasznaloRepository;
    private final TeremRepository teremRepository;
    private final JegyRepository jegyRepository;
    private final FilmRepository filmRepository;
    private final NapiBeosztasRepository beosztasRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        if (moziRepository.existsMozi()) {
            log.info("A mintaadat-feltöltés kimarad: az adatbázis már tartalmaz adatokat.");
            return;
        }
        moziRepository.save(new MoziEntity("Minta Mozi", "Debrecen", "Minta utca 1.", 2004));

        JogEntity admin = jogRepository.save(new JogEntity(null, "ADMIN", "Mozi adminisztrátor"));
        JogEntity felhasznalo = jogRepository.save(new JogEntity(null, "FELHASZNALO", "Regisztrált mozilátogató"));
        felhasznalo("Minta Admin", "admin@mozi.hu", "admin1234", LocalDate.of(1985, 3, 12), admin);
        felhasznalo("Minta Anna", "demo@mozi.hu", "demo1234", LocalDate.of(2001, 7, 4), felhasznalo);

        TeremEntity nagy = terem("Nagyterem", 0, 120, VetitesTipus.HAROM_D);
        TeremEntity kek = terem("Kék terem", 1, 60, VetitesTipus.KET_D);
        TeremEntity kamara = terem("Kamaraterem", 1, 30, VetitesTipus.KET_D);

        // Pontosan négyféle jegy létezik; a feltételt a JegyTipus határozza meg.
        JegyEntity teljes = jegyRepository.save(new JegyEntity(JegyTipus.TELJES_ARU, 2990));
        jegyRepository.save(new JegyEntity(JegyTipus.DIAK, 2390));
        jegyRepository.save(new JegyEntity(JegyTipus.NYUGDIJAS, 2190));
        jegyRepository.save(new JegyEntity(JegyTipus.GYEREK, 1990));

        LocalDate ma = LocalDate.now(clock);
        FilmEntity f1 = film("A csillagok őrzője", ma.minusDays(20), 142, "sci-fi", "USA");
        FilmEntity f2 = film("Nyári zápor", ma.minusDays(6), 98, "romantikus", "Magyarország");
        FilmEntity f3 = film("Az utolsó állomás", ma.minusDays(40), 117, "thriller", "Franciaország");

        for (int nap = 0; nap < 3; nap++) {
            LocalDate datum = ma.plusDays(nap);
            vetites(f1, nagy, datum, LocalTime.of(14, 0), true, teljes);
            vetites(f1, nagy, datum, LocalTime.of(18, 0), false, teljes);
            vetites(f2, kek, datum, LocalTime.of(16, 30), false, teljes);
            vetites(f2, kek, datum, LocalTime.of(20, 15), false, teljes);
            vetites(f3, kamara, datum, LocalTime.of(19, 0), true, teljes);
            vetites(f3, kamara, datum, LocalTime.of(21, 45), true, teljes);
        }
        log.info("Bemutató adatok betöltve (demo@mozi.hu / demo1234).");
    }

    private void felhasznalo(String nev, String email, String jelszo, LocalDate szul, JogEntity jog) {
        FelhasznaloEntity f = new FelhasznaloEntity();
        f.setNev(nev);
        f.setEmail(email);
        f.setJelszo(passwordEncoder.encode(jelszo));
        f.setSzuletesiDatum(szul);
        f.setJog(jog);
        felhasznaloRepository.save(f);
    }

    private TeremEntity terem(String nev, int emelet, int ferohely, VetitesTipus tipus) {
        TeremEntity t = new TeremEntity();
        t.setNev(nev);
        t.setEmelet(emelet);
        t.setFerohely(ferohely);
        t.setVetitesTipus(tipus);
        return teremRepository.save(t);
    }

    private FilmEntity film(String cim, LocalDate bemutato, int hossz, String mufaj, String orszag) {
        FilmEntity f = new FilmEntity();
        f.setCim(cim);
        f.setBemutatoDatum(bemutato);
        f.setHosszPerc(hossz);
        f.setMufaj(mufaj);
        f.setGyartoOrszag(orszag);
        f.setForgalmazo("Minta Forgalmazó Kft.");
        f.setGyartoCeg("Minta Filmstúdió");
        f.setFoszereplo("Minta Színész");
        f.setRendezo("Minta Rendező");
        return filmRepository.save(f);
    }

    private void vetites(FilmEntity film, TeremEntity terem, LocalDate datum, LocalTime kezdes,
                         boolean eredetiNyelvu, JegyEntity jegy) {
        NapiBeosztasEntity b = new NapiBeosztasEntity();
        b.setFilm(film);
        b.setTerem(terem);
        b.setDatum(datum);
        b.setKezdes(kezdes);
        LocalTime vege = kezdes.plusMinutes(film.getHosszPerc() + 15L);
        b.setBefejezes(vege.isBefore(kezdes) ? LocalTime.of(23, 59) : vege);
        b.setEredetiNyelvu(eredetiNyelvu);
        b.setJegy(jegy);
        beosztasRepository.save(b);
    }
}
