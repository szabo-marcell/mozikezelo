package hu.unideb.inf.mozikezelo.service.exception;

import java.time.Duration;

// Túl sok sikertelen bejelentkezési kísérlet: 429 Too Many Requests, Retry-After fejléccel.
public class TulSokProbalkozasException extends RuntimeException {
    private final long varakozasMasodperc;

    public TulSokProbalkozasException(Duration hatralevo) {
        super("Túl sok sikertelen bejelentkezési kísérlet.");
        // Felfelé kerekítünk, hogy a kliens ne próbálkozzon túl korán.
        this.varakozasMasodperc = Math.max(1, (hatralevo.toMillis() + 999) / 1000);
    }

    public long getVarakozasMasodperc() {
        return varakozasMasodperc;
    }
}
