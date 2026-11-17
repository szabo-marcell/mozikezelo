package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.repository.FelhasznaloRepository;
import hu.unideb.inf.mozikezelo.service.dto.FelhasznaloResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.mapper.FelhasznaloMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FelhasznaloService {
    private final FelhasznaloRepository felhasznalok;
    private final FelhasznaloMapper mapper;

    public FelhasznaloResponse profil(Long felhasznaloId) {
        return felhasznalok.findById(felhasznaloId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ErforrasNotFoundException("A felhasználó nem található."));
    }
}
