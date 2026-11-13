package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.MoziEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoziRepository extends JpaRepository<MoziEntity, Long> {

    default Optional<MoziEntity> findMozi() {
        return findById(MoziEntity.SINGLETON_ID);
    }

    default boolean existsMozi() {
        return existsById(MoziEntity.SINGLETON_ID);
    }
}
