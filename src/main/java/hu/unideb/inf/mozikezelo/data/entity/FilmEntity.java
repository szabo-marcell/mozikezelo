package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "FILM", check = @CheckConstraint(name = "ck_film_hossz", constraint = "hossz_perc > 0"))
@Getter
@Setter
@NoArgsConstructor
public class FilmEntity extends AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String cim;

    // Mikor került a mozikba.
    @Column(name = "bemutato_datum", nullable = false)
    private LocalDate bemutatoDatum;

    @Min(1)
    @Column(name = "hossz_perc", nullable = false)
    private int hosszPerc;

    @Column(nullable = false, length = 60)
    private String mufaj;

    @Column(name = "gyarto_orszag", nullable = false, length = 100)
    private String gyartoOrszag;

    @Column(nullable = false, length = 150)
    private String forgalmazo;

    @Column(name = "gyarto_ceg", nullable = false, length = 150)
    private String gyartoCeg;

    @Column(nullable = false, length = 100)
    private String foszereplo;

    @Column(nullable = false, length = 100)
    private String rendezo;
}
