package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FelbehagyottRendelesEntity;
import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import hu.unideb.inf.mozikezelo.data.entity.JegyEntity;
import hu.unideb.inf.mozikezelo.data.entity.NapiBeosztasEntity;
import hu.unideb.inf.mozikezelo.data.repository.FelbehagyottRendelesRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyfoglalasRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.FelbehagyottRendelesResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyfoglalasRequest;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Félbehagyott rendelések: a "Tovább a fizetéshez" lépésnél mentődnek, a sikeres fizetés törli őket.
 * Ugyanarra az előadásra és székre a felhasználónak csak egy félbehagyott rendelése lehet.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FelbehagyottRendelesService {

    private final FelbehagyottRendelesRepository rendelesek;
    private final JegyfoglalasRepository foglalasok;
    private final JegyfoglalasService foglalasService;
    private final MoziMapper mapper;
    private final Clock clock;

    public List<FelbehagyottRendelesResponse> sajat(Long felhasznaloId) {
        return rendelesek.findAllByFelhasznalo_IdOrderByModositvaDesc(felhasznaloId).stream()
                .map(this::valasz).toList();
    }

    public FelbehagyottRendelesResponse get(Long felhasznaloId, Long id) {
        return valasz(rendeles(felhasznaloId, id));
    }

    // Új félbehagyott rendelés; ha ugyanarra az előadásra és székre már van, azt frissíti.
    @Transactional
    public FelbehagyottRendelesResponse ment(Long felhasznaloId, JegyfoglalasRequest request) {
        NapiBeosztasEntity beosztas = foglalasService.eloadasKeresese(request);
        FelbehagyottRendelesEntity rendeles = rendelesek
                .findByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzam(felhasznaloId, beosztas.getId(), request.szekSzam())
                .orElseGet(FelbehagyottRendelesEntity::new);
        return kitolt(rendeles, felhasznaloId, beosztas, request);
    }

    // A rendelés módosítása (pl. másik jegytípus vagy szék a fizetés előtt).
    @Transactional
    public FelbehagyottRendelesResponse modosit(Long felhasznaloId, Long id, JegyfoglalasRequest request) {
        FelbehagyottRendelesEntity rendeles = rendeles(felhasznaloId, id);
        NapiBeosztasEntity beosztas = foglalasService.eloadasKeresese(request);
        // Ha ugyanerre az előadásra és székre már van másik félbehagyott rendelés, azt összevonjuk ezzel.
        rendelesek.findByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzam(felhasznaloId, beosztas.getId(), request.szekSzam())
                .filter(masik -> !masik.getId().equals(id))
                .ifPresent(masik -> {
                    rendelesek.delete(masik);
                    rendelesek.flush();
                });
        return kitolt(rendeles, felhasznaloId, beosztas, request);
    }

    @Transactional
    public void torol(Long felhasznaloId, Long id) {
        rendelesek.delete(rendeles(felhasznaloId, id));
    }

    private FelbehagyottRendelesResponse kitolt(FelbehagyottRendelesEntity rendeles, Long felhasznaloId,
                                                NapiBeosztasEntity beosztas, JegyfoglalasRequest request) {
        FelhasznaloEntity felhasznalo = foglalasService.felhasznalo(felhasznaloId);
        JegyEntity jegy = foglalasService.jegy(request.jegyNev());
        foglalasService.ellenoriz(beosztas, felhasznalo, jegy, request.szekSzam());
        rendeles.setFelhasznalo(felhasznalo);
        rendeles.setNapiBeosztas(beosztas);
        rendeles.setJegy(jegy);
        rendeles.setSzekSzam(request.szekSzam());
        return valasz(rendelesek.saveAndFlush(rendeles));
    }

    private FelbehagyottRendelesEntity rendeles(Long felhasznaloId, Long id) {
        // Más felhasználó rendeléséről sem áruljuk el, hogy létezik.
        return rendelesek.findByIdAndFelhasznalo_Id(id, felhasznaloId)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található saját félbehagyott rendelés: " + id));
    }

    // Folytatható, ha az előadás még nem kezdődött el, és a széket közben senki sem foglalta le.
    private FelbehagyottRendelesResponse valasz(FelbehagyottRendelesEntity r) {
        boolean folytathato = !NapiBeosztasService.elkezdodott(r.getNapiBeosztas(), clock)
                && !foglalasok.existsByNapiBeosztas_IdAndSzekSzam(r.getNapiBeosztas().getId(), r.getSzekSzam());
        return mapper.felbehagyott(r, folytathato);
    }
}
