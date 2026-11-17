package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FilmEntity;
import hu.unideb.inf.mozikezelo.data.entity.JegyEntity;
import hu.unideb.inf.mozikezelo.data.entity.NapiBeosztasEntity;
import hu.unideb.inf.mozikezelo.data.entity.TeremEntity;
import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import hu.unideb.inf.mozikezelo.data.repository.FelbehagyottRendelesRepository;
import hu.unideb.inf.mozikezelo.data.repository.FilmRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyfoglalasRepository;
import hu.unideb.inf.mozikezelo.data.repository.NapiBeosztasRepository;
import hu.unideb.inf.mozikezelo.data.repository.TeremRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.EloadasReszletekResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.NapiBeosztasRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.NapiBeosztasResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.exception.UzletiSzabalyException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NapiBeosztasService {

    /** A film előtti reklám/előzetes ideje: a vetítés vége = kezdés + film hossza + ennyi perc. */
    public static final int PLUSZ_PERC = 15;

    private final NapiBeosztasRepository beosztasok;
    private final JegyfoglalasRepository foglalasok;
    private final FelbehagyottRendelesRepository felbehagyottak;
    private final FilmRepository filmek;
    private final TeremRepository termek;
    private final JegyRepository jegyek;
    private final JegyFeltetelEllenorzo jegyFeltetel;
    private final MoziMapper mapper;
    private final Clock clock;

    public List<NapiBeosztasResponse> napra(LocalDate datum) {
        return beosztasok.findAllByDatumOrderByKezdes(datum).stream().map(mapper::beosztas).toList();
    }

    /**
     * Egy előadás kiválasztása: a foglaláshoz szükséges szabad székek és jegytípusok.
     * Bejelentkezett felhasználónak (szuletesiDatum != null) csak az általa igényelhető jegyeket listázzuk.
     */
    public EloadasReszletekResponse reszletek(Long id, LocalDate szuletesiDatum) {
        NapiBeosztasEntity beosztas = eloadas(id);
        var foglalt = new HashSet<>(foglalasok.foglaltSzekek(id));
        int ferohely = beosztas.getTerem().getFerohely();
        List<Integer> szabad = IntStream.rangeClosed(1, ferohely).boxed().filter(sz -> !foglalt.contains(sz)).toList();
        var response = mapper.beosztas(beosztas);
        return new EloadasReszletekResponse(
                response,
                ferohely,
                szabad,
                jegyek.findAll(Sort.by("ar", "nev")).stream()
                        .filter(j -> szuletesiDatum == null || jegyFeltetel.igenyelheto(j, szuletesiDatum))
                        .map(mapper::jegy).toList(),
                !elkezdodott(beosztas, clock) && !szabad.isEmpty(),
                response.foglalasUrl());
    }

    /**
     * Új vetítés felvétele (ADMIN). A befejezés a kezdésből, a film hosszából és a plusz időből számolódik.
     * Ugyanabban a teremben, ugyanazon a napon két vetítés nem fedheti át egymást
     * (az egyik vége és a másik kezdete egybeeshet).
     */
    @Transactional
    public NapiBeosztasResponse letrehoz(NapiBeosztasRequest request) {
        FilmEntity film = filmek.findById(request.filmId())
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található film: " + request.filmId()));
        TeremEntity terem = termek.findByIdZarolva(request.teremId())
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található terem: " + request.teremId()));
        JegyEntity jegy = request.jegyId() == null
                ? jegyek.findByTipus(JegyTipus.TELJES_ARU)
                        .orElseThrow(() -> new ErforrasNotFoundException("Nem található a teljes árú jegytípus."))
                : jegyek.findById(request.jegyId())
                        .orElseThrow(() -> new ErforrasNotFoundException("Nem található jegytípus: " + request.jegyId()));

        LocalDate datum = request.datum();
        LocalTime kezdes = request.kezdes().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime kezdet = LocalDateTime.of(datum, kezdes);
        if (!kezdet.isAfter(LocalDateTime.now(clock))) {
            throw new UzletiSzabalyException("Múltbeli vagy már elkezdődött időpontra nem vehető fel vetítés.");
        }
        LocalDateTime vege = kezdet.plusMinutes((long) film.getHosszPerc() + PLUSZ_PERC);
        if (!vege.toLocalDate().equals(datum) || vege.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            throw new UzletiSzabalyException("A vetítésnek éjfél előtt véget kell érnie (a(z) \"" + film.getCim()
                    + "\" " + film.getHosszPerc() + " + " + PLUSZ_PERC + " perc).");
        }
        LocalTime befejezes = vege.toLocalTime();

        List<NapiBeosztasEntity> utkozok = beosztasok.utkozoVetitesek(terem.getId(), datum, kezdes, befejezes);
        if (!utkozok.isEmpty()) {
            String lista = utkozok.stream()
                    .map(b -> "\"" + b.getFilm().getCim() + "\" " + b.getKezdes() + "–" + b.getBefejezes())
                    .collect(Collectors.joining(", "));
            throw new UzletiSzabalyException("Időpont-ütközés a(z) " + terem.getNev() + " teremben ("
                    + kezdes + "–" + befejezes + "): " + lista + ".");
        }

        NapiBeosztasEntity beosztas = new NapiBeosztasEntity();
        beosztas.setFilm(film);
        beosztas.setTerem(terem);
        beosztas.setDatum(datum);
        beosztas.setKezdes(kezdes);
        beosztas.setBefejezes(befejezes);
        beosztas.setEredetiNyelvu(film.getFoszinkronszinesz() == null || request.eredetiNyelvu());
        beosztas.setJegy(jegy);
        return mapper.beosztas(beosztasok.saveAndFlush(beosztas));
    }

    /**
     * Vetítés törlése (ADMIN). Már elkezdődött vagy lefoglalt jeggyel rendelkező vetítés nem törölhető;
     * a rá szóló félbehagyott (ki nem fizetett) rendelések a vetítéssel együtt törlődnek.
     */
    @Transactional
    public void torol(Long id) {
        NapiBeosztasEntity beosztas = eloadas(id);
        if (elkezdodott(beosztas, clock)) {
            throw new UzletiSzabalyException("Már elkezdődött vagy lezajlott vetítés nem törölhető.");
        }
        if (foglalasok.existsByNapiBeosztas_Id(id)) {
            throw new UzletiSzabalyException("A vetítés nem törölhető, mert már van rá jegyfoglalás.");
        }
        felbehagyottak.deleteAllByNapiBeosztas_Id(id);
        beosztasok.delete(beosztas);
    }

    NapiBeosztasEntity eloadas(Long id) {
        return beosztasok.findById(id)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található előadás: " + id));
    }

    static boolean elkezdodott(NapiBeosztasEntity beosztas, Clock clock) {
        return !LocalDateTime.of(beosztas.getDatum(), beosztas.getKezdes()).isAfter(LocalDateTime.now(clock));
    }
}
