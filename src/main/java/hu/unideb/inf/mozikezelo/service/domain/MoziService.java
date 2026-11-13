package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.data.repository.MoziRepository;
import hu.unideb.inf.mozikezelo.service.dto.domain.MoziResponse;
import hu.unideb.inf.mozikezelo.service.exception.ErforrasNotFoundException;
import hu.unideb.inf.mozikezelo.service.mapper.MoziMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MoziService {
    private final MoziRepository mozi;
    private final MoziMapper mapper;

    public MoziResponse alapadatok() {
        return mozi.findMozi().map(mapper::mozi)
                .orElseThrow(() -> new ErforrasNotFoundException("A mozi alapadatai még nincsenek rögzítve."));
    }
}
