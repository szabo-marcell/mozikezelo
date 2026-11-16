package hu.unideb.inf.mozikezelo.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "FELHASZNALO", uniqueConstraints = {
        @UniqueConstraint(name = "uk_felhasznalo_email", columnNames = "email")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FelhasznaloEntity
        extends AuditEntity
        implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nev", nullable = false, length = 100)
    private String nev;
    @Column(name = "email", nullable = false, length = 254)
    private String email;
    @Column(name = "jelszo", nullable = false, length = 255)
    private String jelszo;
    @Column(name = "szulDat", nullable = false)
    private LocalDate szuletesiDatum;

    // A JWT-k "ver" claimje ehhez igazodik: növelésekor (pl. kijelentkezéskor) a korábban kiadott tokenek érvénytelenné válnak.
    @Column(name = "token_verzio", nullable = false)
    private int tokenVerzio;

    // FELHASZNALO N:1 JOG
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jogosultsag_id")
    private JogEntity jog;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(jog);
    }

    @Override
    public @Nullable String getPassword() {
        return jelszo;
    }

    // A bejelentkezési azonosító az e-mail cím.
    @Override
    public String getUsername() {
        return email;
    }
}
