package hu.unideb.inf.mozikezelo.data.entity;

import hu.unideb.inf.mozikezelo.data.entity.enums.VetitesTipus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "TEREM", uniqueConstraints = @UniqueConstraint(name = "uk_terem_nev", columnNames = "nev"),
        check = @CheckConstraint(name = "ck_terem_ferohely", constraint = "ferohely > 0"))
@Getter
@Setter
@NoArgsConstructor
public class TeremEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nev;

    @Column(nullable = false)
    private int emelet;

    @Min(1)
    @Column(nullable = false)
    private int ferohely;

    @Enumerated(EnumType.STRING)
    @Column(name = "vetites_tipus", nullable = false, length = 10)
    private VetitesTipus vetitesTipus;
}
