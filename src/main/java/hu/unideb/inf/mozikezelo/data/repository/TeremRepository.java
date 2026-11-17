package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.TeremEntity;
import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeremRepository extends JpaRepository<TeremEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TeremEntity t WHERE t.id = :id")
    Optional<TeremEntity> findByIdZarolva(@Param("id") Long id);

    Optional<TeremEntity> findByNev(String nev);

    boolean existsByNev(String nev);

    boolean existsByNevAndIdNot(String nev, Long id);

    List<TeremEntity> findAllByVetitesTipus(VetitesTipus vetitesTipus);

    List<TeremEntity> findAllByEmelet(int emelet);
}
