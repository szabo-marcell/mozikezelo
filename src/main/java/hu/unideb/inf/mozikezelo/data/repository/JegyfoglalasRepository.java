package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.JegyfoglalasEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JegyfoglalasRepository extends JpaRepository<JegyfoglalasEntity, Long> {

    @EntityGraph(attributePaths = {"film", "terem", "jegy", "napiBeosztas"})
    Page<JegyfoglalasEntity> findAllByFelhasznalo_Id(Long felhasznaloId, Pageable pageable);

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    Optional<JegyfoglalasEntity> findByIdAndFelhasznalo_Id(Long id, Long felhasznaloId);

    @EntityGraph(attributePaths = {"film", "terem", "jegy", "napiBeosztas", "felhasznalo"})
    Optional<JegyfoglalasEntity> findByFoglalasKod(String foglalasKod);

    // Visszamondás az összes foglalási adat megadásával.
    @EntityGraph(attributePaths = {"film", "terem", "jegy", "napiBeosztas"})
    Optional<JegyfoglalasEntity> findByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzamAndJegy_Id(
            Long felhasznaloId, Long napiBeosztasId, int szekSzam, Long jegyId);

    @Query("SELECT f.szekSzam FROM JegyfoglalasEntity f WHERE f.napiBeosztas.id = :beosztasId")
    List<Integer> foglaltSzekek(@Param("beosztasId") Long beosztasId);

    boolean existsByNapiBeosztas_IdAndSzekSzam(Long napiBeosztasId, int szekSzam);

    boolean existsByFoglalasKod(String foglalasKod);

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
