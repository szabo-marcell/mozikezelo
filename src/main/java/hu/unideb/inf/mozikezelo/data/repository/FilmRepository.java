package hu.unideb.inf.mozikezelo.data.repository;

import hu.unideb.inf.mozikezelo.data.entity.FilmEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FilmRepository extends JpaRepository<FilmEntity, Long> {

    Page<FilmEntity> findAllByCimContainingIgnoreCase(String cim, Pageable pageable);

    List<FilmEntity> findAllByMufajIgnoreCase(String mufaj);

    List<FilmEntity> findAllByRendezoIgnoreCase(String rendezo);

    List<FilmEntity> findAllByBemutatoDatumBetween(LocalDate tol, LocalDate ig);
}
