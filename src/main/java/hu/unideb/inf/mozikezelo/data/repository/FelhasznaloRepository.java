package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.FelhasznaloEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FelhasznaloRepository extends JpaRepository<FelhasznaloEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "jog")
    Page<FelhasznaloEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "jog")
    Page<FelhasznaloEntity> findAllByNevContainingIgnoreCase(String nev, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "jog")
    Optional<FelhasznaloEntity> findById(Long id);

    @EntityGraph(attributePaths = "jog")
    Optional<FelhasznaloEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<FelhasznaloEntity> findAllByJog_Nev(String jogNev);

    // A felhasználó összes korábban kiadott tokenjét érvényteleníti.
    @Modifying(clearAutomatically = true)
    @Query("update FelhasznaloEntity f set f.tokenVerzio = f.tokenVerzio + 1 where f.id = :id")
    int tokenVerzioNovelese(@Param("id") Long id);
}
