package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.TeremEntity;
import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeremRepository extends JpaRepository<TeremEntity, Long> {

    Optional<TeremEntity> findByNev(String nev);

    boolean existsByNev(String nev);

    boolean existsByNevAndIdNot(String nev, Long id);

    List<TeremEntity> findAllByVetitesTipus(VetitesTipus vetitesTipus);

    List<TeremEntity> findAllByEmelet(int emelet);
}
