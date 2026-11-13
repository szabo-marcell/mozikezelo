package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.entity.FilmEntity;
import hu.unideb.inf.mozikezelo.data.repository.FilmRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.FilmResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
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
        return filmek.findById(id).map(mapper::film)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található film: " + id));
    }
}
