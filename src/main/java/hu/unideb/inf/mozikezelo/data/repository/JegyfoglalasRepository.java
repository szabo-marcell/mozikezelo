package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.JegyfoglalasEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JegyfoglalasRepository extends JpaRepository<JegyfoglalasEntity, Long> {

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    Page<JegyfoglalasEntity> findAllByFelhasznalo_Id(Long felhasznaloId, Pageable pageable);

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    Optional<JegyfoglalasEntity> findByIdAndFelhasznalo_Id(Long id, Long felhasznaloId);

    @EntityGraph(attributePaths = {"felhasznalo", "terem", "jegy"})
    List<JegyfoglalasEntity> findAllByFilm_Id(Long filmId);

    @EntityGraph(attributePaths = {"felhasznalo", "film", "jegy"})
    List<JegyfoglalasEntity> findAllByTerem_Id(Long teremId);

    long countByFilm_IdAndTerem_Id(Long filmId, Long teremId);

    boolean existsByFelhasznalo_Id(Long felhasznaloId);

    boolean existsByFilm_Id(Long filmId);

    boolean existsByTerem_Id(Long teremId);

    boolean existsByJegy_Id(Long jegyId);
}
