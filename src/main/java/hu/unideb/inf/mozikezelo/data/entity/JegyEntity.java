package hu.unideb.inf.mozikezelo.data.entity;

import hu.unideb.inf.mozikezelo.data.entity.enums.JegyTipus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Jegytípus. Csak négyféle létezhet (Gyerek, Diák, Nyugdíjas, Teljes árú):
 * a tipus oszlop enum és egyedi, így minden típusból legfeljebb egy rekord van.
 */
@Entity
@Table(name = "JEGY", uniqueConstraints = {
        @UniqueConstraint(name = "uk_jegy_nev", columnNames = "nev"),
        @UniqueConstraint(name = "uk_jegy_tipus", columnNames = "tipus")
},
        check = @CheckConstraint(name = "ck_jegy_ar", constraint = "ar >= 0"))
@Getter
@Setter
@NoArgsConstructor
public class JegyEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JegyTipus tipus;

    @Column(nullable = false, length = 100)
    private String nev;

    @Min(0)
    @Column(nullable = false)
    private int ar;


    @Column(nullable = false, length = 255)
    private String feltetel;

    public JegyEntity(JegyTipus tipus, int ar) {
        this.tipus = tipus;
        this.nev = tipus.getMegnevezes();
        this.feltetel = tipus.getFeltetel();
        this.ar = ar;
    }
}
