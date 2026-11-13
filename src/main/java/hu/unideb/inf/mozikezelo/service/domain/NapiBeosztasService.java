package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.NapiBeosztasEntity;
import hu.unideb.inf.mozikezelo.data.repository.JegyRepository;
import hu.unideb.inf.mozikezelo.data.repository.JegyfoglalasRepository;
import hu.unideb.inf.mozikezelo.data.repository.NapiBeosztasRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.EloadasReszletekResponse;
import hu.unideb.inf.mozikezelo.service.dto.domain.NapiBeosztasResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NapiBeosztasService {
    private final NapiBeosztasRepository beosztasok;
    private final JegyfoglalasRepository foglalasok;
    private final JegyRepository jegyek;
    private final MoziMapper mapper;
    private final Clock clock;

    public List<NapiBeosztasResponse> napra(LocalDate datum) {
        return beosztasok.findAllByDatumOrderByKezdes(datum).stream().map(mapper::beosztas).toList();
    }

    // Egy előadás kiválasztása: a foglaláshoz szükséges szabad székek és jegytípusok.
    public EloadasReszletekResponse reszletek(Long id) {
        NapiBeosztasEntity beosztas = eloadas(id);
        var foglalt = new HashSet<>(foglalasok.foglaltSzekek(id));
        int ferohely = beosztas.getTerem().getFerohely();
        List<Integer> szabad = IntStream.rangeClosed(1, ferohely).boxed().filter(sz -> !foglalt.contains(sz)).toList();
        var response = mapper.beosztas(beosztas);
        return new EloadasReszletekResponse(
                response,
                ferohely,
                szabad,
                jegyek.findAll(Sort.by("ar", "nev")).stream().map(mapper::jegy).toList(),
                !elkezdodott(beosztas, clock) && !szabad.isEmpty(),
                response.foglalasUrl());
    }

    NapiBeosztasEntity eloadas(Long id) {
        return beosztasok.findById(id)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található előadás: " + id));
    }

    static boolean elkezdodott(NapiBeosztasEntity beosztas, Clock clock) {
        return !LocalDateTime.of(beosztas.getDatum(), beosztas.getKezdes()).isAfter(LocalDateTime.now(clock));
    }
}
