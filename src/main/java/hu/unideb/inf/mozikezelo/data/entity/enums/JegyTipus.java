package hu.unideb.inf.mozikezelo.data.entity.enums;

/**
 * A mozi négyféle jegytípusa és igénylési feltételük.
 * Az életkort csak a születési év alapján számoljuk: kor = aktuális év - születési év.
 */
public enum JegyTipus {
    GYEREK("Gyerek", "0 és 17 év közötti néző (a születési év alapján)"),
    DIAK("Diák", "6 és 23 év közötti néző (a születési év alapján)"),
    NYUGDIJAS("Nyugdíjas", "65 éves vagy idősebb néző (a születési év alapján)"),
    TELJES_ARU("Teljes árú", "Bárki megveheti");

    public static final int GYEREK_MAX_KOR = 17;
    public static final int DIAK_MIN_KOR = 6;
    public static final int DIAK_MAX_KOR = 23;
    public static final int NYUGDIJAS_MIN_KOR = 65;

    private final String megnevezes;
    private final String feltetel;

    JegyTipus(String megnevezes, String feltetel) {
        this.megnevezes = megnevezes;
        this.feltetel = feltetel;
    }

    public String getMegnevezes() {
        return megnevezes;
    }

    public String getFeltetel() {
        return feltetel;
    }

    /** Igényelheti-e az adott korú (aktuális év - születési év) néző ezt a jegytípust. */
    public boolean igenyelheto(int kor) {
        return switch (this) {
            case GYEREK -> kor >= 0 && kor <= GYEREK_MAX_KOR;
            case DIAK -> kor >= DIAK_MIN_KOR && kor <= DIAK_MAX_KOR;
            case NYUGDIJAS -> kor >= NYUGDIJAS_MIN_KOR;
            case TELJES_ARU -> true;
        };
    }
}
