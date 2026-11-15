package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.data.entity.JegyEntity;
import hu.unideb.inf.mozikezelo.data.entity.JegyfoglalasEntity;
import hu.unideb.inf.mozikezelo.data.entity.NapiBeosztasEntity;
import hu.unideb.inf.mozikezelo.data.repository.FelbehagyottRendelesRepository;
import hu.unideb.inf.mozikezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyfoglalasRepository;
import hu.unideb.inf.mozikezelo.data.repository.NapiBeosztasRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.BelepoJegyResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.EloadasFoglalasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.exception.HibasKeresException;
import hu.unideb.inf.mozikezelo.service.exception.UzletiSzabalyException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JegyfoglalasService {
    private static final String KOD_KARAKTEREK = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JegyfoglalasRepository foglalasok;
    private final FelbehagyottRendelesRepository felbehagyottak;
    private final NapiBeosztasRepository beosztasok;
    private final JegyRepository jegyek;
    private final FelhasznaloRepository felhasznalok;
    private final NapiBeosztasService beosztasService;
    private final JegyFeltetelEllenorzo jegyFeltetel;
    private final MoziMapper mapper;
    private final Clock clock;

    public Page<JegyfoglalasResponse> sajatFoglalasok(Long felhasznaloId, Pageable page) {
        Pageable rendezett = PageRequest.of(page.getPageNumber(), Math.min(page.getPageSize(), 100), Sort.by("id").descending());
        return foglalasok.findAllByFelhasznalo_Id(felhasznaloId, rendezett).map(mapper::foglalas);
    }

    // Foglalás az összes adat megadásával: film, időpont, jegytípus, szék (és szükség esetén terem).
    @Transactional
    public JegyfoglalasResponse foglal(Long felhasznaloId, JegyfoglalasRequest request) {
        NapiBeosztasEntity beosztas = eloadasKeresese(request);
        return foglalEloadasra(felhasznaloId, beosztas, request.jegyNev(), request.szekSzam());
    }

    // Foglalás a napi beosztásból kiválasztott előadásra: csak a jegytípus és a szék kell.
    @Transactional
    public JegyfoglalasResponse foglalEloadasra(Long felhasznaloId, Long beosztasId, EloadasFoglalasRequest request) {
        NapiBeosztasEntity beosztas = beosztasService.eloadas(beosztasId);
        return foglalEloadasra(felhasznaloId, beosztas, request.jegyNev(), request.szekSzam());
    }

    // Visszamondás a speciális rendelésazonosítóval.
    @Transactional
    public void lemondKoddal(Long felhasznaloId, String foglalasKod) {
        JegyfoglalasEntity foglalas = foglalasok.findByFoglalasKod(foglalasKod.strip().toUpperCase(Locale.ROOT))
                // Más felhasználó foglalásáról sem áruljuk el, hogy létezik.
                .filter(f -> f.getFelhasznalo().getId().equals(felhasznaloId))
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját foglalás ezzel az azonosítóval: " + foglalasKod));
        lemond(foglalas);
    }

    // Visszamondás az összes foglalási adat megadásával.
    @Transactional
    public void lemondAdatokkal(Long felhasznaloId, JegyfoglalasRequest request) {
        NapiBeosztasEntity beosztas = eloadasKeresese(request);
        JegyEntity jegy = jegy(request.jegyNev());
        JegyfoglalasEntity foglalas = foglalasok
                .findByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzamAndJegy_Id(
                        felhasznaloId, beosztas.getId(), request.szekSzam(), jegy.getId())
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját foglalás a megadott adatokkal."));
        lemond(foglalas);
    }

    // "Jegyeim": a felhasználó összes kifizetett jegye, időrendben.
    public List<BelepoJegyResponse> jegyeim(Long felhasznaloId) {
        return foglalasok.findAllByFelhasznalo_IdOrderByNapiBeosztas_DatumAscNapiBeosztas_KezdesAsc(felhasznaloId)
                .stream().map(this::belepoJegy).toList();
    }

    // Egy saját jegy a foglalási kód alapján (a teljes képernyős jegynézethez).
    public BelepoJegyResponse jegyKoddal(Long felhasznaloId, String foglalasKod) {
        return foglalasok.findByFoglalasKod(foglalasKod.strip().toUpperCase(Locale.ROOT))
                .filter(f -> f.getFelhasznalo().getId().equals(felhasznaloId))
                .map(this::belepoJegy)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját jegy ezzel az azonosítóval: " + foglalasKod));
    }

    private BelepoJegyResponse belepoJegy(JegyfoglalasEntity foglalas) {
        NapiBeosztasEntity b = foglalas.getNapiBeosztas();
        boolean ervenyes = LocalDateTime.of(b.getDatum(), b.getBefejezes()).isAfter(LocalDateTime.now(clock));
        return mapper.belepoJegy(foglalas, ervenyes);
    }

    private JegyfoglalasResponse foglalEloadasra(Long felhasznaloId, NapiBeosztasEntity beosztas, String jegyNev, int szekSzam) {
        FelhasznaloEntity felhasznalo = felhasznalo(felhasznaloId);
        JegyEntity jegy = jegy(jegyNev);
        ellenoriz(beosztas, felhasznalo, jegy, szekSzam);
        if (foglalasok.existsByNapiBeosztas_IdAndSzekSzam(beosztas.getId(), szekSzam)) {
            throw new UzletiSzabalyException("A(z) " + szekSzam + ". szék erre az előadásra már foglalt.");
        }
        JegyfoglalasEntity foglalas = new JegyfoglalasEntity();
        foglalas.setFoglalasKod(ujKod());
        foglalas.setNapiBeosztas(beosztas);
        foglalas.setFilm(beosztas.getFilm());
        foglalas.setTerem(beosztas.getTerem());
        foglalas.setJegy(jegy);
        foglalas.setFelhasznalo(felhasznalo);
        foglalas.setSzekSzam(szekSzam);
        // A párhuzamos foglalást az egyedi (előadás, szék) kényszer is kiszűri: 409-es válasz.
        JegyfoglalasResponse valasz = mapper.foglalas(foglalasok.saveAndFlush(foglalas));
        // A kifizetett rendelés már nem félbehagyott.
        felbehagyottak.deleteAllByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzam(felhasznaloId, beosztas.getId(), szekSzam);
        return valasz;
    }

    /**
     * A foglalás és a félbehagyott rendelés közös szabályai: az előadás még nem kezdődött el,
     * a szék létezik a teremben, és a felhasználó igényelheti a jegytípust.
     */
    void ellenoriz(NapiBeosztasEntity beosztas, FelhasznaloEntity felhasznalo, JegyEntity jegy, int szekSzam) {
        if (NapiBeosztasService.elkezdodott(beosztas, clock)) {
            throw new UzletiSzabalyException("Már elkezdődött vagy lezajlott előadásra nem lehet jegyet foglalni.");
        }
        int ferohely = beosztas.getTerem().getFerohely();
        if (szekSzam < 1 || szekSzam > ferohely) {
            throw new HibasKeresException("A szék száma 1 és " + ferohely + " között lehet ebben a teremben.");
        }
        if (!jegyFeltetel.igenyelheto(jegy, felhasznalo.getSzuletesiDatum())) {
            throw new UzletiSzabalyException("A(z) \"" + jegy.getNev() + "\" jegy nem igényelhető. Feltétel: "
                    + jegy.getFeltetel() + ". A születési év alapján számolt életkor: "
                    + jegyFeltetel.kor(felhasznalo.getSzuletesiDatum()) + " év.");
        }
    }

    FelhasznaloEntity felhasznalo(Long felhasznaloId) {
        return felhasznalok.findById(felhasznaloId)
                .orElseThrow(() -> new ErforrasNotFoundException("A felhasználó nem található."));
    }

    private void lemond(JegyfoglalasEntity foglalas) {
        if (NapiBeosztasService.elkezdodott(foglalas.getNapiBeosztas(), clock)) {
            throw new UzletiSzabalyException("Már elkezdődött vagy lezajlott előadás foglalása nem mondható vissza.");
        }
        foglalasok.delete(foglalas);
    }

    NapiBeosztasEntity eloadasKeresese(JegyfoglalasRequest request) {
        List<NapiBeosztasEntity> talalatok = beosztasok.findAllByFilm_CimIgnoreCaseAndDatumAndKezdes(
                request.filmCim().strip(), request.datum(), request.kezdes());
        if (request.teremNev() != null && !request.teremNev().isBlank()) {
            String teremNev = request.teremNev().strip();
            talalatok = talalatok.stream().filter(b -> b.getTerem().getNev().equalsIgnoreCase(teremNev)).toList();
        }
        if (talalatok.isEmpty()) {
            throw new ErforrasNotFoundException("Nincs ilyen előadás: " + request.filmCim() + ", "
                    + request.datum() + " " + request.kezdes());
        }
        if (talalatok.size() > 1) {
            throw new HibasKeresException("A film ebben az időpontban több teremben is megy, add meg a terem nevét is.");
        }
        return talalatok.getFirst();
    }

    JegyEntity jegy(String nev) {
        return jegyek.findByNev(nev.strip())
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található jegytípus: " + nev));
    }

    private String ujKod() {
        String kod;
        do {
            StringBuilder sb = new StringBuilder("MZ-");
            for (int i = 0; i < 8; i++) {
                sb.append(KOD_KARAKTEREK.charAt(RANDOM.nextInt(KOD_KARAKTEREK.length())));
            }
            kod = sb.toString();
        } while (foglalasok.existsByFoglalasKod(kod));
        return kod;
    }
}
