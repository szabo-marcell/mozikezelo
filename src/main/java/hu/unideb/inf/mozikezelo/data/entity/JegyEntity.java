package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "JEGY", uniqueConstraints = @UniqueConstraint(name = "uk_jegy_nev", columnNames = "nev"),
        check = @CheckConstraint(name = "ck_jegy_ar", constraint = "ar >= 0"))
@Getter
@Setter
@NoArgsConstructor
public class JegyEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nev;

    // Ár forintban.
    @Min(0)
    @Column(nullable = false)
    private int ar;

    // Milyen feltétel kell az igényléshez (pl. érvényes diákigazolvány).
    @Column(length = 255)
    private String feltetel;
}
