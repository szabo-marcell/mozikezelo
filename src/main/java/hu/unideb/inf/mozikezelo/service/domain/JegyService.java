package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.repository.JegyRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.JegyResponse;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JegyService {
    private final JegyRepository jegyek;
    private final MoziMapper mapper;

    // Az összes jegytípus ár szerint rendezve (a foglalási űrlap jegyválasztójához).
    public List<JegyResponse> osszes() {
        return jegyek.findAll(Sort.by("ar", "nev")).stream().map(mapper::jegy).toList();
    }
}
