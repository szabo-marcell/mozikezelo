package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "JEGYFOGLALAS",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_foglalas_eloadas_szek", columnNames = {"napi_beosztas_id", "szek_szam"}),
                @UniqueConstraint(name = "uk_foglalas_kod", columnNames = "foglalas_kod")
        },
        indexes = {
                @Index(name = "idx_foglalas_felhasznalo", columnList = "felhasznalo_id"),
                @Index(name = "idx_foglalas_film", columnList = "film_id"),
                @Index(name = "idx_foglalas_terem", columnList = "terem_id")
        },
        check = @CheckConstraint(name = "ck_foglalas_szek", constraint = "szek_szam > 0"))
@Getter
@Setter
@NoArgsConstructor
public class JegyfoglalasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "foglalas_kod", nullable = false, length = 20)
    private String foglalasKod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terem_id", nullable = false)
    private TeremEntity terem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felhasznalo_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jegy_id", nullable = false)
    private JegyEntity jegy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "napi_beosztas_id", nullable = false)
    private NapiBeosztasEntity napiBeosztas;

    @Min(1)
    @Column(name = "szek_szam", nullable = false)
    private int szekSzam;
}
