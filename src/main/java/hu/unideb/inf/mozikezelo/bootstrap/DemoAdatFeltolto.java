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
 * Üres adatbázis feltöltése bemutató adatokkal: mozi, termek, jegytípusok, a mozikban jelenleg
 * vetített filmek, a mai és a következő két nap beosztása, valamint két felhasználó.
 * Demo belépés: demo@mozi.hu / demo1234, admin@mozi.hu / admin1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoAdatFeltolto implements CommandLineRunner {

    private static final String PLAKATOK = "/img/plakatok/";
    private static final String LOGOK = "/img/filmek/";
    private static final int REKLAM_PERC = 15;
    private static final int TAKARITAS_PERC = 15;
    private static final int UTOLSO_BEFEJEZES_PERC = 23 * 60 + 45;
    private static final LocalTime ESTI_EREDETI_NYELVU = LocalTime.of(20, 0);

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
        moziRepository.save(new MoziEntity("Dynamic Cinema", "Debrecen", "Lumiére u. 7.", 2004));

        JogEntity admin = jogRepository.save(new JogEntity(null, "ADMIN", "Mozi adminisztrátor"));
        JogEntity felhasznalo = jogRepository.save(new JogEntity(null, "FELHASZNALO", "Regisztrált mozilátogató"));
        felhasznalo("Minta Admin", "admin@mozi.hu", "admin1234", LocalDate.of(1985, 3, 12), admin);
        felhasznalo("Minta Anna", "demo@mozi.hu", "demo1234", LocalDate.of(2001, 7, 4), felhasznalo);

        TeremEntity nagy = terem("Nagyterem", 0, 120, VetitesTipus.HAROM_D);
        TeremEntity kek = terem("Kék terem", 1, 60, VetitesTipus.KET_D);
        TeremEntity kamara = terem("Kamaraterem", 1, 30, VetitesTipus.KET_D);

        JegyEntity teljes = jegyRepository.save(new JegyEntity(JegyTipus.TELJES_ARU, 2990));
        jegyRepository.save(new JegyEntity(JegyTipus.DIAK, 2390));
        jegyRepository.save(new JegyEntity(JegyTipus.NYUGDIJAS, 2190));
        jegyRepository.save(new JegyEntity(JegyTipus.GYEREK, 1990));

        FilmEntity hogyan2 = film("Hogyan tudnék élni nélküled? 2.", LocalDate.of(2026, 11, 12), 114,
                "romantikus", "Magyarország", "InterCom", "IKO Production",
                "Törőcsik Franciska", "Dyga Zsombor", null, "hogyan-tudnek-elni-nelkuled-2.png",
                "Demjén Ferenc dalaira épülő zenés romantikus vígjáték folytatása. Az első rész szereplőinek "
                        + "élete újabb fordulatot vesz: szerelem, barátság és családi kalamajkák, "
                        + "közösen énekelhető slágerekkel.");
        FilmEntity ebenezer = film("Ebenezer", LocalDate.of(2026, 11, 12), 116,
                "családi", "USA", "UIP-Duna Film", "Paramount Pictures",
                "Johnny Depp", "Ti West", "Stohl András", "ebenezer.webp",
                "Charles Dickens Karácsonyi énekének új mozis feldolgozása. A zsugori és magának való "
                        + "Ebenezer Scrooge-ot szenteste éjjelén három szellem látogatja meg, hogy megmutassák "
                        + "neki múltját, jelenét és jövőjét, és talán egy utolsó esélyt is adjanak neki.");
        FilmEntity agyagpofa = film("Agyagpofa", LocalDate.of(2026, 10, 22), 108,
                "testhorror", "USA", "InterCom", "DC Studios",
                "Tom Rhys-Harries", "James Watkins", "Brasch Bence", "agyagpofa.webp",
                "Matt Hagen, a feltörekvő színész karrierje egy brutális támadás után egy csapásra véget ér: "
                        + "eltorzult arcát egy kétes kísérleti kezelés hozza helyre, ám a teste ettől kezdve "
                        + "formálható agyaggá válik. A DC-univerzum sötét, testhorror-elemekkel teli eredettörténete.");
        FilmEntity orokseg = film("Az örökség", LocalDate.of(2026, 10, 22), 90,
                "thriller", "Magyarország", "Vertigo Média", "Független magyar produkció",
                "Mucsi Zoltán", "Tasnádi István", null, "az-orokseg.webp",
                "Egy özvegy apa a megtakarításait egy nála jóval fiatalabb nőre kezdi költeni, amivel felnőtt "
                        + "gyerekei gyanúját is felkelti, és a családi feszültségből hamarosan bűnügy lesz. "
                        + "Tasnádi István, az Aranyélet vezető írójának első rendezése nyolc év után jutott mozikba.");
        FilmEntity residentEvil = film("Resident Evil", LocalDate.of(2026, 9, 17), 95,
                "horror", "USA", "InterCom", "Constantin Film / Sony Pictures",
                "Austin Abrams", "Zach Cregger", "Kerek Dávid", "resident-evil.jpg",
                "A legendás Capcom videojáték-sorozat új mozis feldolgozása a Barbár rendezőjétől. Egy egyszerű "
                        + "futár egyetlen éjszaka alatt kénytelen átvágni a járvány sújtotta Raccoon Cityn, "
                        + "miközben egyre közelebb kerül az Umbrella vállalat sötét titkaihoz.");
        FilmEntity kalapos = film("Kalapos Macska", LocalDate.of(2026, 11, 5), 110,
                "családi", "USA", "InterCom", "Warner Bros. Pictures Animation",
                "Bill Hader", "Alessandro Carloni", "Miller Zoltán", "kalapos-macska.webp",
                "Dr. Seuss klasszikus meséjének animációs feldolgozása. Egy esős délutánon két unatkozó "
                        + "testvérhez beállít a csíkos kalapot viselő, kiszámíthatatlan macska, és a ház "
                        + "pillanatok alatt a feje tetejére áll.");
        FilmEntity vadlo = film("Vadló Kilenc", LocalDate.of(2026, 11, 5), 118,
                "dráma", "Egyesült Királyság / USA", "Fórum Hungary", "Searchlight Pictures",
                "John Malkovich", "Martin McDonagh", "Hegedűs D. Géza", "vadlo-kilenc.png",
                "Martin McDonagh (Három óriásplakát Ebbing határában, A sziget szellemei) fekete humorral "
                        + "átszőtt drámája titkosügynökökről, akiknek egy régi bevetés következményeivel "
                        + "kell szembenézniük.");
        FilmEntity kivansag = film("Még egy kívánság", LocalDate.of(2026, 10, 15), 105,
                "romantikus", "Magyarország", "Vertigo Média", "Megafilm Service",
                "Hermányi Mariann", "Dobó Kata", null, "meg-egy-kivansag.webp",
                "Dobó Kata romantikus vígjátéka a szerelemről, a második esélyekről és arról, mennyi "
                        + "bátorság kell egy újabb kívánsághoz. A főszerepekben Hermányi Mariann és "
                        + "Kádár L. Gellért.");
        FilmEntity moulin = film("Moulin", LocalDate.of(2026, 11, 5), 130,
                "történelmi", "Franciaország", "Mozinet", "Montmartre Films / TF1 Studio",
                "Gilles Lellouche", "Nemes László", null, "moulin.jpg",
                "Nemes László első francia nyelvű filmje Jean Moulinnak, a francia ellenállás vezetőjének "
                        + "utolsó napjait mutatja be, miután 1943-ban a Gestapo fogságába esik. "
                        + "A 2026-os cannes-i fesztivál versenyfilmje, eredeti nyelven, magyar felirattal.");

        LocalDate ma = LocalDate.now(clock);
        for (int nap = 0; nap < 3; nap++) {
            LocalDate datum = ma.plusDays(nap);
            napiMusor(nagy, datum, LocalTime.of(10, 0), teljes,
                    kalapos, ebenezer, agyagpofa, residentEvil, ebenezer);
            napiMusor(kek, datum, LocalTime.of(10, 30), teljes,
                    hogyan2, kivansag, kalapos, hogyan2, vadlo);
            napiMusor(kamara, datum, LocalTime.of(11, 0), teljes,
                    orokseg, moulin, vadlo, orokseg, residentEvil);
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

    private FilmEntity film(String cim, LocalDate bemutato, int hossz, String mufaj, String orszag,
                            String forgalmazo, String gyartoCeg, String foszereplo, String rendezo,
                            String foszinkronszinesz, String plakat, String leiras) {
        FilmEntity f = new FilmEntity();
        f.setCim(cim);
        f.setBemutatoDatum(bemutato);
        f.setHosszPerc(hossz);
        f.setMufaj(mufaj);
        f.setGyartoOrszag(orszag);
        f.setForgalmazo(forgalmazo);
        f.setGyartoCeg(gyartoCeg);
        f.setFoszereplo(foszereplo);
        f.setRendezo(rendezo);
        f.setFoszinkronszinesz(foszinkronszinesz);
        f.setKepUrl(PLAKATOK + plakat);
        f.setLogoUrl(LOGOK + plakat.substring(0, plakat.lastIndexOf('.')) + ".svg");
        f.setLeiras(leiras);
        return filmRepository.save(f);
    }

    /**
     * Egy terem egynapos műsora: a filmek egymás után következnek, két előadás között takarítással,
     * a kezdések negyedórára kerekítve. Ami már nem fér bele a napba, az kimarad.
     */
    private void napiMusor(TeremEntity terem, LocalDate datum, LocalTime elso, JegyEntity jegy,
                           FilmEntity... filmek) {
        int kezdes = elso.toSecondOfDay() / 60;
        for (FilmEntity film : filmek) {
            int vege = kezdes + REKLAM_PERC + film.getHosszPerc();
            if (vege > UTOLSO_BEFEJEZES_PERC) {
                return;
            }
            vetites(film, terem, datum, LocalTime.of(kezdes / 60, kezdes % 60),
                    LocalTime.of(vege / 60, vege % 60), jegy);
            int kovetkezo = vege + TAKARITAS_PERC;
            kezdes = (kovetkezo + 14) / 15 * 15;
        }
    }

    private void vetites(FilmEntity film, TeremEntity terem, LocalDate datum, LocalTime kezdes,
                         LocalTime befejezes, JegyEntity jegy) {
        NapiBeosztasEntity b = new NapiBeosztasEntity();
        b.setFilm(film);
        b.setTerem(terem);
        b.setDatum(datum);
        b.setKezdes(kezdes);
        b.setBefejezes(befejezes);
        b.setEredetiNyelvu(film.getFoszinkronszinesz() == null || !kezdes.isBefore(ESTI_EREDETI_NYELVU));
        b.setJegy(jegy);
        beosztasRepository.save(b);
    }
}
