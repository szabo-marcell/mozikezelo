package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.FelbehagyottRendelesEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface FelbehagyottRendelesRepository extends JpaRepository<FelbehagyottRendelesEntity, Long> {

    @EntityGraph(attributePaths = {"napiBeosztas", "napiBeosztas.film", "napiBeosztas.terem", "jegy"})
    List<FelbehagyottRendelesEntity> findAllByFelhasznalo_IdOrderByModositvaDesc(Long felhasznaloId);

    @EntityGraph(attributePaths = {"napiBeosztas", "napiBeosztas.film", "napiBeosztas.terem", "jegy"})
    Optional<FelbehagyottRendelesEntity> findByIdAndFelhasznalo_Id(Long id, Long felhasznaloId);

    Optional<FelbehagyottRendelesEntity> findByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzam(
            Long felhasznaloId, Long napiBeosztasId, int szekSzam);

    @Modifying
    void deleteAllByFelhasznalo_IdAndNapiBeosztas_IdAndSzekSzam(Long felhasznaloId, Long napiBeosztasId, int szekSzam);

    @Modifying
    void deleteAllByNapiBeosztas_Id(Long napiBeosztasId);
}
