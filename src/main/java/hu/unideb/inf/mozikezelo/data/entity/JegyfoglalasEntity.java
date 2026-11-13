package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "JEGYFOGLALAS", indexes = {
        @Index(name = "idx_foglalas_felhasznalo", columnList = "felhasznalo_id"),
        @Index(name = "idx_foglalas_film", columnList = "film_id"),
        @Index(name = "idx_foglalas_terem", columnList = "terem_id")
})
@Getter
@Setter
@NoArgsConstructor
public class JegyfoglalasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FILM 1:N JEGYFOGLALAS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    // TEREM 1:N JEGYFOGLALAS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terem_id", nullable = false)
    private TeremEntity terem;

    // FELHASZNALO 1:N JEGYFOGLALAS
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felhasznalo_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    // A foglalt jegytípus
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jegy_id", nullable = false)
    private JegyEntity jegy;
}
