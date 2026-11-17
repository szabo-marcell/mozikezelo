fejlec("beosztas.html");
const NAPOK_SZAMA = 7;
const valaszto = document.getElementById("nap");
const lista = document.getElementById("vetitesek");
const doboz = document.getElementById("uzenet");

const ma = new Date();
for (let i = 0; i < NAPOK_SZAMA; i++) {
    const d = new Date(ma.getFullYear(), ma.getMonth(), ma.getDate() + i);
    const iso = isoDatum(d);
    const opcio = elem("option", "", (i === 0 ? "Ma – " : i === 1 ? "Holnap – " : "") + szepDatum(iso));
    opcio.value = iso;
    valaszto.append(opcio);
}
const kert = parameter("datum");
if (kert && [...valaszto.options].some(o => o.value === kert)) valaszto.value = kert;

valaszto.addEventListener("change", () => {
    history.replaceState(null, "", "?datum=" + valaszto.value);
    betolt();
});

async function betolt() {
    lista.replaceChildren();
    uzenet(doboz, "");
    try {
        const vetitesek = await api("/api/napi-beosztas?datum=" + encodeURIComponent(valaszto.value));
        if (!vetitesek.length) {
            lista.append(elem("p", "ures", "Erre a napra nincs vetítés."));
            return;
        }
        const most = new Date();
        for (const v of vetitesek) lista.append(vetitesKartya(v, most));
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

function vetitesKartya(v, most) {
    const [e, h, n] = v.datum.split("-").map(Number);
    const [ora, perc] = v.kezdes.split(":").map(Number);
    const elmult = new Date(e, h - 1, n, ora, perc) <= most;

    const kartya = elem(elmult ? "div" : "a", "kartya" + (elmult ? " lejart" : ""));
    if (!elmult) kartya.href = "foglalas.html?eloadas=" + v.id;

    const fej = elem("div", "kartya-fej");
    fej.append(elem("h2", "", v.filmCim), elem("span", "idopont", ido(v.kezdes) + " – " + ido(v.befejezes)));

    const cimkek = elem("div", "cimkek");
    cimkek.append(
        elem("span", "cimke kiemelt", vetitesTipus(v.vetitesTipus)),
        elem("span", "cimke", v.eredetiNyelvu ? "Eredeti nyelvű (feliratos)" : "Szinkronizált"),
        elem("span", "cimke", v.hosszPerc + " perc"));

    kartya.append(fej, cimkek, adatlista([
        ["Terem", v.teremNev],
        ["Jegy", v.jegyNev],
    ]), elem("p", "tipp", elmult ? "Ez a vetítés már elkezdődött." : "Kattints a helyfoglaláshoz →"));
    return kartya;
}

betolt();
