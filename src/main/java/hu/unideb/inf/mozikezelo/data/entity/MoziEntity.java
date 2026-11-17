package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A mozi alapadatai (pl. Dynamic Cinema, Debrecen, Lumiére u. 7., 2004).
 * Singleton entitás: a táblában pontosan egy rekord lehet, fix azonosítóval.
 * Különálló entitás, más entitással nem áll kapcsolatban; a kezdőlap adatait szolgáltatja.
 */
@Entity
@Table(name = "MOZI", check = {
        @CheckConstraint(name = "ck_mozi_singleton", constraint = "id = 1"),
        @CheckConstraint(name = "ck_mozi_epites_eve", constraint = "epites_eve > 0")
})
@Getter
@Setter
@NoArgsConstructor
public class MoziEntity extends AuditEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Setter(AccessLevel.NONE)
    private Long id = SINGLETON_ID;

    @Column(nullable = false, length = 100)
    private String nev;

    @Column(nullable = false, length = 100)
    private String varos;

    @Column(nullable = false, length = 150)
    private String utca;

    @Min(1)
    @Column(name = "epites_eve", nullable = false)
    private int epitesEve;

    public MoziEntity(String nev, String varos, String utca, int epitesEve) {
        this.nev = nev;
        this.varos = varos;
        this.utca = utca;
        this.epitesEve = epitesEve;
    }
}
