package hu.unideb.inf.mozikezelo.service.domain;

import hu.unideb.inf.mozikezelo.service.exception.TulSokProbalkozasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Védelem a jelszavak végigpróbálgatása ellen.
 * <ul>
 *     <li>E-mail címenként: {@code max-hibas-email} sikertelen kísérlet az ablakon belül ideiglenesen zárolja a fiókot.</li>
 *     <li>IP-címenként: {@code max-hibas-ip} sikertelen kísérlet zárolja a címet (sok e-mail cím végigpróbálása ellen).</li>
 * </ul>
 * A zárolás nem létező e-mail címre is ugyanígy működik, így a válaszból nem derül ki, hogy létezik-e a fiók.
 * A zárolás alatt helyes jelszóval sem lehet belépni, különben a támadó a zárolás alatt is tovább próbálgathatna.
 * Az adatok memóriában vannak: újraindításkor törlődnek, és több szerverpéldány nem osztja meg őket.
 */
@Component
public class BejelentkezesVedelem {

    private static final int TAKARITASI_KUSZOB = 10_000;

    private final Map<String, Szamlalo> emailSzerint = new ConcurrentHashMap<>();
    private final Map<String, Szamlalo> ipSzerint = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxHibasEmail;
    private final int maxHibasIp;
    private final Duration ablak;
    private final Duration zarolas;

    public BejelentkezesVedelem(Clock clock,
                                @Value("${app.bejelentkezes.max-hibas-email:5}") int maxHibasEmail,
                                @Value("${app.bejelentkezes.max-hibas-ip:20}") int maxHibasIp,
                                @Value("${app.bejelentkezes.ablak-perc:15}") long ablakPerc,
                                @Value("${app.bejelentkezes.zarolas-perc:15}") long zarolasPerc) {
        this.clock = clock;
        this.maxHibasEmail = maxHibasEmail;
        this.maxHibasIp = maxHibasIp;
        this.ablak = Duration.ofMinutes(ablakPerc);
        this.zarolas = Duration.ofMinutes(zarolasPerc);
    }

    /** A bejelentkezés előtt hívandó: zárolt e-mail cím vagy IP-cím esetén kivételt dob. */
    public void ellenoriz(String email, String ip) {
        Instant most = clock.instant();
        Duration hatra = hatralevo(emailSzerint.get(email), most);
        Duration ipHatra = hatralevo(ipSzerint.get(ip), most);
        if (ipHatra.compareTo(hatra) > 0) {
            hatra = ipHatra;
        }
        if (!hatra.isZero()) {
            throw new TulSokProbalkozasException(hatra);
        }
    }

    public void sikertelen(String email, String ip) {
        Instant most = clock.instant();
        emailSzerint.compute(email, (k, sz) -> novel(sz, maxHibasEmail, most));
        ipSzerint.compute(ip, (k, sz) -> novel(sz, maxHibasIp, most));
        takarit(emailSzerint, most);
        takarit(ipSzerint, most);
    }

    /** Sikeres belépés után a fiók számlálója nullázódik. Az IP-címé nem, hogy egy saját fiókkal ne lehessen nullázni. */
    public void sikeres(String email) {
        emailSzerint.remove(email);
    }

    private Szamlalo novel(Szamlalo sz, int max, Instant most) {
        int hibak = sz == null || lejart(sz, most) ? 1 : sz.hibak() + 1;
        Instant ablakKezdete = sz == null || lejart(sz, most) ? most : sz.ablakKezdete();
        if (hibak >= max) {
            return new Szamlalo(0, most, most.plus(zarolas));
        }
        return new Szamlalo(hibak, ablakKezdete, sz == null ? null : sz.zarolvaEddig());
    }

    private boolean lejart(Szamlalo sz, Instant most) {
        boolean zarolt = sz.zarolvaEddig() != null && sz.zarolvaEddig().isAfter(most);
        return !zarolt && !sz.ablakKezdete().plus(ablak).isAfter(most);
    }

    private static Duration hatralevo(Szamlalo sz, Instant most) {
        if (sz == null || sz.zarolvaEddig() == null || !sz.zarolvaEddig().isAfter(most)) {
            return Duration.ZERO;
        }
        return Duration.between(most, sz.zarolvaEddig());
    }

    private void takarit(Map<String, Szamlalo> map, Instant most) {
        if (map.size() > TAKARITASI_KUSZOB) {
            map.values().removeIf(sz -> lejart(sz, most));
        }
    }

    private record Szamlalo(int hibak, Instant ablakKezdete, Instant zarolvaEddig) {
    }
}
