package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.NapiBeosztasEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface NapiBeosztasRepository extends JpaRepository<NapiBeosztasEntity, Long> {

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    List<NapiBeosztasEntity> findAllByDatumOrderByKezdes(LocalDate datum);

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    List<NapiBeosztasEntity> findAllByDatumAndTerem_IdOrderByKezdes(LocalDate datum, Long teremId);

    @EntityGraph(attributePaths = {"film", "terem", "jegy"})
    List<NapiBeosztasEntity> findAllByFilm_IdAndDatumGreaterThanEqualOrderByDatumAscKezdesAsc(Long filmId, LocalDate datum);

    boolean existsByFilm_Id(Long filmId);

    boolean existsByTerem_Id(Long teremId);

    boolean existsByJegy_Id(Long jegyId);

    // Ütközik-e egy új vetítés az adott terem aznapi beosztásával.
    @Query("""
            SELECT COUNT(b) > 0 FROM NapiBeosztasEntity b
            WHERE b.terem.id = :teremId AND b.datum = :datum
              AND b.kezdes < :befejezes AND b.befejezes > :kezdes
            """)
    boolean letezikUtkozes(@Param("teremId") Long teremId,
                           @Param("datum") LocalDate datum,
                           @Param("kezdes") LocalTime kezdes,
                           @Param("befejezes") LocalTime befejezes);
}
