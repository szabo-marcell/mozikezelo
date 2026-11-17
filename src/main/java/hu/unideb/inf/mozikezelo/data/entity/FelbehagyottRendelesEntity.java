package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Félbehagyott rendelés: a felhasználó kitöltötte a foglalást (előadás, jegytípus, szék),
 * de a fizetést nem fejezte be. Sikeres fizetéskor törlődik; addig folytatható vagy törölhető.
 */
@Entity
@Table(name = "FELBEHAGYOTT_RENDELES",
        uniqueConstraints = @UniqueConstraint(name = "uk_felbehagyott_felhasznalo_eloadas_szek",
                columnNames = {"felhasznalo_id", "napi_beosztas_id", "szek_szam"}),
        indexes = @Index(name = "idx_felbehagyott_felhasznalo", columnList = "felhasznalo_id"),
        check = @CheckConstraint(name = "ck_felbehagyott_szek", constraint = "szek_szam > 0"))
@Getter
@Setter
@NoArgsConstructor
public class FelbehagyottRendelesEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "felhasznalo_id", nullable = false)
    private FelhasznaloEntity felhasznalo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "napi_beosztas_id", nullable = false)
    private NapiBeosztasEntity napiBeosztas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jegy_id", nullable = false)
    private JegyEntity jegy;

    @Min(1)
    @Column(name = "szek_szam", nullable = false)
    private int szekSzam;
}
