package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.repository.TeremRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.TeremResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeremService {
    private final TeremRepository termek;
    private final MoziMapper mapper;

    public List<TeremResponse> osszes() {
        return termek.findAll(Sort.by("nev")).stream().map(mapper::terem).toList();
    }

    public TeremResponse azonositoSzerint(Long id) {
        return termek.findById(id).map(mapper::terem)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található terem ezzel az azonosítóval: " + id));
    }

    public TeremResponse nevSzerint(String nev) {
        return termek.findByNev(nev.strip()).map(mapper::terem)
                .orElseThrow(() -> new ErforrasNotFoundException("Nem található terem ezzel a névvel: " + nev));
    }
}
