package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Kapcsolótábla a FILM és a TEREM között (N:N): egy adott napon
 * melyik teremben melyik filmet vetítik, mettől meddig.
 */
@Entity
@Table(name = "NAPI_BEOSZTAS",
        uniqueConstraints = @UniqueConstraint(name = "uk_beosztas_terem_kezdes",
                columnNames = {"terem_id", "datum", "kezdes"}),
        indexes = {
                @Index(name = "idx_beosztas_film", columnList = "film_id"),
                @Index(name = "idx_beosztas_datum", columnList = "datum")
        },
        check = @CheckConstraint(name = "ck_beosztas_idopont", constraint = "kezdes < befejezes"))
@Getter
@Setter
@NoArgsConstructor
public class NapiBeosztasEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "terem_id", nullable = false)
    private TeremEntity terem;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(nullable = false)
    private LocalTime kezdes;

    @Column(nullable = false)
    private LocalTime befejezes;

    @Column(name = "eredeti_nyelvu", nullable = false)
    private boolean eredetiNyelvu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jegy_id", nullable = false)
    private JegyEntity jegy;
}
