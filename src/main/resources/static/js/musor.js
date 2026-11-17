fejlec("musor.html");

const PLUSZ_PERC = 15;
const doboz = document.getElementById("uzenet");
const tartalom = document.getElementById("tartalom");
const napMezo = document.getElementById("nap");
const urlap = document.getElementById("vetites-urlap");
const filmMezo = document.getElementById("film");
const teremMezo = document.getElementById("terem");
const kezdesMezo = document.getElementById("kezdes");
const jegyMezo = document.getElementById("jegy");
const nyelvMezo = document.getElementById("eredetiNyelvu");
const idotartamDoboz = document.getElementById("idotartam");
const utkozesDoboz = document.getElementById("utkozes");
const hozzaadasGomb = document.getElementById("hozzaadas");
const termekLista = document.getElementById("termek");

let filmek = [];
let termek = [];
let vetitesek = [];

const perc = t => { const [o, p] = t.split(":").map(Number); return o * 60 + p; };
const idoPercbol = p => String(Math.floor(p / 60)).padStart(2, "0") + ":" + String(p % 60).padStart(2, "0");

if (bejelentkezesSzukseges()) indit();

async function indit() {
    try {
        const profil = await api("/api/felhasznalo/profil");
        localStorage.setItem(JOG_KULCS, profil.jogNev || "");
        if (profil.jogNev !== "ADMIN") {
            uzenet(doboz, "Ez az oldal csak adminisztrátoroknak érhető el.");
            return;
        }
        [filmek, termek] = await Promise.all([api("/api/filmek"), api("/api/termek")]);
        const jegyek = await api("/api/jegyek");
        valasztoFeltoltese(jegyek);

        const ma = isoDatum(new Date());
        napMezo.min = ma;
        const kert = parameter("datum");
        napMezo.value = kert && kert >= ma ? kert : ma;
        tartalom.hidden = false;
        await betolt();
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

function valasztoFeltoltese(jegyek) {
    filmMezo.replaceChildren(Object.assign(elem("option", "", "– Válassz filmet –"), { value: "" }),
        ...filmek.map(f => Object.assign(elem("option", "", f.cim + " (" + f.hosszPerc + " perc)"), { value: f.id })));
    teremMezo.replaceChildren(Object.assign(elem("option", "", "– Válassz termet –"), { value: "" }),
        ...termek.map(t => Object.assign(elem("option", "",
            t.nev + " – " + vetitesTipus(t.vetitesTipus) + ", " + t.ferohely + " hely, " + t.emelet + ". emelet"),
            { value: t.id })));
    jegyMezo.replaceChildren(...jegyek.map(j => Object.assign(
        elem("option", "", j.nev + " – " + forint(j.ar)), { value: j.id, selected: j.tipus === "TELJES_ARU" })));
}

napMezo.addEventListener("change", () => { if (napMezo.value) betolt(); });
document.getElementById("elozo-nap").addEventListener("click", () => napLeptetes(-1));
document.getElementById("kovetkezo-nap").addEventListener("click", () => napLeptetes(1));

function napLeptetes(irany) {
    const [e, h, n] = napMezo.value.split("-").map(Number);
    const uj = isoDatum(new Date(e, h - 1, n + irany));
    if (uj < napMezo.min) return;
    napMezo.value = uj;
    betolt();
}

async function betolt() {
    history.replaceState(null, "", "?datum=" + napMezo.value);
    document.getElementById("lista-cim").textContent = "Beosztás – " + szepDatum(napMezo.value);
    try {
        vetitesek = await api("/api/napi-beosztas?datum=" + encodeURIComponent(napMezo.value));
        megjelenit();
        ellenoriz();
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

function megjelenit() {
    termekLista.replaceChildren();
    const most = new Date();
    for (const t of termek) {
        const sajat = vetitesek.filter(v => v.teremId === t.id);
        const kartya = elem("div", "kartya");
        const fej = elem("div", "kartya-fej");
        fej.append(elem("h2", "", t.nev), elem("span", "cimke kiemelt", vetitesTipus(t.vetitesTipus)));
        kartya.append(fej);
        if (!sajat.length) {
            kartya.append(elem("p", "tipp", "Erre a napra nincs vetítés ebben a teremben."));
        } else {
            const sorok = elem("ul", "musor-sorok");
            for (const v of sajat) sorok.append(vetitesSor(v, most));
            kartya.append(sorok);
        }
        termekLista.append(kartya);
    }
}

function vetitesSor(v, most) {
    const [e, h, n] = v.datum.split("-").map(Number);
    const [o, p] = v.kezdes.split(":").map(Number);
    const elmult = new Date(e, h - 1, n, o, p) <= most;

    const sor = elem("li", "musor-sor" + (elmult ? " elhalvanyitott" : ""));
    const adatok = elem("div");
    adatok.append(elem("span", "idopont", ido(v.kezdes) + " – " + ido(v.befejezes)), " ",
        elem("strong", "", v.filmCim));
    const cimkek = elem("div", "cimkek");
    cimkek.append(elem("span", "cimke", v.eredetiNyelvu ? "Eredeti nyelvű" : "Szinkronizált"),
        elem("span", "cimke", v.jegyNev));
    adatok.append(cimkek);

    const torles = elem("button", "gomb veszely kicsi", "Törlés");
    torles.type = "button";
    if (elmult) {
        torles.disabled = true;
        torles.title = "Már elkezdődött vagy lezajlott vetítés nem törölhető.";
    }
    torles.addEventListener("click", () => torol(v, torles));
    sor.append(adatok, torles);
    return sor;
}

async function torol(v, gomb) {
    if (!confirm("Biztosan törlöd a vetítést?\n\n" + v.filmCim + "\n" + v.teremNev + ", " + szepDatum(v.datum)
        + " " + ido(v.kezdes) + "–" + ido(v.befejezes) + "\n\nLefoglalt jeggyel rendelkező vetítés nem törölhető.")) return;
    gomb.disabled = true;
    try {
        await api("/api/napi-beosztas/" + v.id, { method: "DELETE" });
        uzenet(doboz, "A vetítést töröltük: " + v.filmCim + ", " + v.teremNev + " " + ido(v.kezdes) + ".", "siker");
        await betolt();
    } catch (h) {
        uzenet(doboz, h.message);
        gomb.disabled = false;
    }
}

for (const m of [filmMezo, teremMezo, kezdesMezo]) m.addEventListener("input", ellenoriz);
filmMezo.addEventListener("change", nyelvBeallitasa);
teremMezo.addEventListener("change", kezdesJavaslat);

function kivalasztottFilm() {
    return filmek.find(f => String(f.id) === filmMezo.value);
}

function nyelvBeallitasa() {
    const f = kivalasztottFilm();
    const tipp = document.getElementById("nyelv-tipp");
    if (f && !f.foszinkronszinesz) {
        nyelvMezo.checked = true;
        nyelvMezo.disabled = true;
        tipp.textContent = "Ennek a filmnek nincs szinkronja, ezért eredeti nyelven megy.";
    } else {
        nyelvMezo.disabled = false;
        tipp.textContent = f ? "Szinkron: " + f.foszinkronszinesz : "";
    }
}

function kezdesJavaslat() {
    if (kezdesMezo.value || !teremMezo.value) return ellenoriz();
    const sajat = vetitesek.filter(v => String(v.teremId) === teremMezo.value);
    const utolsoVege = sajat.length ? Math.max(...sajat.map(v => perc(v.befejezes))) : perc("10:00");
    const javasolt = Math.ceil(utolsoVege / 5) * 5;
    if (javasolt < 24 * 60) kezdesMezo.value = idoPercbol(javasolt);
    ellenoriz();
}

/** Kiszámolja a vetítés végét, és megnézi, ütközik-e a terem más vetítésével. Igazat ad, ha felvehető. */
function ellenoriz() {
    const f = kivalasztottFilm();
    uzenet(idotartamDoboz, "", "info");
    uzenet(utkozesDoboz, "");
    if (!f || !kezdesMezo.value) {
        hozzaadasGomb.disabled = false;
        return true;
    }
    const kezd = perc(kezdesMezo.value);
    const vege = kezd + f.hosszPerc + PLUSZ_PERC;
    const hibak = [];

    if (vege >= 24 * 60) {
        hibak.push("A vetítés éjfél után érne véget – válassz korábbi kezdést.");
    } else {
        uzenet(idotartamDoboz, "Vége: " + idoPercbol(vege) + " (" + f.hosszPerc + " perc film + " + PLUSZ_PERC
            + " perc reklám/előzetes)", "info");
    }
    const most = new Date();
    if (napMezo.value === isoDatum(most) && kezd <= most.getHours() * 60 + most.getMinutes()) {
        hibak.push("Ez az időpont már elmúlt.");
    }
    if (teremMezo.value) {
        const utkozok = vetitesek.filter(v => String(v.teremId) === teremMezo.value
            && perc(v.kezdes) < vege && perc(v.befejezes) > kezd);
        if (utkozok.length) {
            hibak.push("Ebben a teremben ekkor már megy: " + utkozok.map(v =>
                "„" + v.filmCim + "” " + ido(v.kezdes) + "–" + ido(v.befejezes)).join(", ") + ".");
        }
    }
    uzenet(utkozesDoboz, hibak.join(" "));
    hozzaadasGomb.disabled = hibak.length > 0;
    return hibak.length === 0;
}

urlap.addEventListener("submit", async e => {
    e.preventDefault();
    if (!urlap.reportValidity() || !ellenoriz()) return;
    const f = kivalasztottFilm();
    const adat = {
        filmId: Number(filmMezo.value),
        teremId: Number(teremMezo.value),
        datum: napMezo.value,
        kezdes: kezdesMezo.value,
        eredetiNyelvu: nyelvMezo.checked,
        jegyId: jegyMezo.value ? Number(jegyMezo.value) : null,
    };
    hozzaadasGomb.disabled = true;
    try {
        const uj = await api("/api/napi-beosztas", { method: "POST", body: adat });
        uzenet(doboz, "Felvettük: " + f.cim + ", " + uj.teremNev + " " + ido(uj.kezdes) + "–" + ido(uj.befejezes) + ".", "siker");
        kezdesMezo.value = "";
        await betolt();
        kezdesJavaslat();
    } catch (h) {
        uzenet(doboz, h.message);
        await betolt();
    } finally {
        hozzaadasGomb.disabled = !ellenoriz();
    }
});
