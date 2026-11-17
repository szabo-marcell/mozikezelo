package hu.unideb.inf.mozikezelo.service.exception;

import java.time.Duration;

public class TulSokProbalkozasException extends RuntimeException {
    private final long varakozasMasodperc;

    public TulSokProbalkozasException(Duration hatralevo) {
        super("Túl sok sikertelen bejelentkezési kísérlet.");
        this.varakozasMasodperc = Math.max(1, (hatralevo.toMillis() + 999) / 1000);
    }

    public long getVarakozasMasodperc() {
        return varakozasMasodperc;
    }
}
