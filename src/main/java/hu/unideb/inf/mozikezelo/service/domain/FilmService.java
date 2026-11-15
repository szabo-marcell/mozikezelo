package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FilmEntity;
import hu.unideb.inf.mozikezelo.data.repository.FilmRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyfoglalasRepository;
import hu.unideb.inf.mozikezelo.data.repository.NapiBeosztasRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmRequest;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.exception.UzletiSzabalyException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilmService {
    private final FilmRepository filmek;
    private final NapiBeosztasRepository beosztasok;
    private final JegyfoglalasRepository foglalasok;
    private final MoziMapper mapper;

    /**
     * Keresés pontos cím (kis-/nagybetű nem számít) és/vagy bemutató dátum szerint.
     * Paraméter nélkül az összes filmet adja vissza cím szerint rendezve.
     */
    public List<FilmResponse> keres(String cim, LocalDate bemutatoDatum) {
        String pontosCim = cim == null || cim.isBlank() ? null : cim.strip();
        List<FilmEntity> talalatok;
        if (pontosCim != null && bemutatoDatum != null) {
            talalatok = filmek.findAllByCimIgnoreCaseAndBemutatoDatum(pontosCim, bemutatoDatum);
        } else if (pontosCim != null) {
            talalatok = filmek.findAllByCimIgnoreCaseOrderByBemutatoDatumDesc(pontosCim);
        } else if (bemutatoDatum != null) {
            talalatok = filmek.findAllByBemutatoDatumOrderByCim(bemutatoDatum);
        } else {
            talalatok = filmek.findAll(Sort.by("cim", "id"));
        }
        return talalatok.stream().map(mapper::film).toList();
    }

    public FilmResponse get(Long id) {
        return mapper.film(film(id));
    }

    // Új film felvétele (ADMIN).
    @Transactional
    public FilmResponse letrehoz(FilmRequest request) {
        FilmEntity film = mapper.ujFilm(normalizal(request));
        return mapper.film(filmek.saveAndFlush(film));
    }

    // Film adatainak módosítása (ADMIN). A napi beosztáshoz és a foglalásokhoz nem nyúlunk.
    @Transactional
    public FilmResponse modosit(Long id, FilmRequest request) {
        FilmEntity film = film(id);
        // A beosztott előadások befejezési ideje a film hosszából számolódott, ezért ilyenkor a hossz nem változhat.
        if (film.getHosszPerc() != request.hosszPerc() && beosztasok.existsByFilm_Id(id)) {
            throw new UzletiSzabalyException(
                    "A film hossza nem módosítható, mert már szerepel a napi beosztásban.");
        }
        mapper.filmFrissites(normalizal(request), film);
        return mapper.film(filmek.saveAndFlush(film));
    }

    /**
     * Film törlése (ADMIN). A napi beosztáshoz és a foglalásokhoz az ADMIN sem nyúlhat,
     * ezért csak olyan film törölhető, amely még nem szerepel egyikben sem.
     */
    @Transactional
    public void torol(Long id) {
        FilmEntity film = film(id);
        if (foglalasok.existsByFilm_Id(id)) {
            throw new UzletiSzabalyException("A film nem törölhető, mert már van rá jegyfoglalás.");
        }
        if (beosztasok.existsByFilm_Id(id)) {
            throw new UzletiSzabalyException("A film nem törölhető, mert szerepel a napi beosztásban.");
        }
        filmek.delete(film);
    }

    private FilmEntity film(Long id) {
        return filmek.findById(id)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található film: " + id));
    }

    private static FilmRequest normalizal(FilmRequest r) {
        return new FilmRequest(r.cim().strip(), r.bemutatoDatum(), r.hosszPerc(), r.mufaj().strip(),
                r.gyartoOrszag().strip(), r.forgalmazo().strip(), r.gyartoCeg().strip(),
                r.foszereplo().strip(), r.rendezo().strip(), uresbolNull(r.foszinkronszinesz()),
                uresbolNull(r.leiras()), uresbolNull(r.kepUrl()));
    }

    private static String uresbolNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
