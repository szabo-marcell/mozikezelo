package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.JegyEntity;
import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JegyRepository extends JpaRepository<JegyEntity, Long> {

    Optional<JegyEntity> findByNev(String nev);

    Optional<JegyEntity> findByTipus(JegyTipus tipus);

    boolean existsByNev(String nev);

    boolean existsByNevAndIdNot(String nev, Long id);
}
