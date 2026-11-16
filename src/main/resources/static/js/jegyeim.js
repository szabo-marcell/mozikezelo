// jegyeim.html oldal szkriptje (a CSP miatt nincs inline JavaScript).
fejlec("jegyeim.html");

const szuro = document.getElementById("nezet");
const lista = document.getElementById("jegyek");
const doboz = document.getElementById("uzenet");
const nezet = document.getElementById("jegy-nezet");
const jegyHelye = document.getElementById("jegy-helye");

let jegyek = [];

if (bejelentkezesSzukseges()) {
    const kertNezet = parameter("nezet");
    if (kertNezet && [...szuro.options].some(o => o.value === kertNezet)) szuro.value = kertNezet;
    betolt();
}

szuro.addEventListener("change", () => {
    history.replaceState(null, "", "?nezet=" + szuro.value);
    megjelenit();
});

async function betolt() {
    try {
        jegyek = await api("/api/jegyfoglalasok/jegyek");
        megjelenit();
        // A foglalás visszaigazolásáról érkezve rögtön megnyitjuk a jegyet.
        const kod = parameter("kod");
        if (kod) {
            const jegy = jegyek.find(j => j.foglalasKod === kod) || await api("/api/jegyfoglalasok/" + encodeURIComponent(kod) + "/jegy");
            megnyit(jegy);
        }
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

function megjelenit() {
    lista.replaceChildren();
    const lathato = jegyek.filter(j => szuro.value === "osszes" || (szuro.value === "kozelgo") === j.ervenyes);
    // A lejártak közül a legutóbbi legyen elöl.
    if (szuro.value === "lejart") lathato.reverse();
    if (!lathato.length) {
        const szoveg = !jegyek.length ? "Még nincs jegyed. Foglalj egyet a napi beosztásból!"
            : szuro.value === "kozelgo" ? "Nincs közelgő előadásra szóló jegyed." : "Nincs lejárt jegyed.";
        const p = elem("p", "ures", szoveg);
        if (!jegyek.length) {
            const a = elem("a", "", " Napi beosztás →");
            a.href = "beosztas.html";
            p.append(a);
        }
        lista.append(p);
        return;
    }
    for (const j of lathato) lista.append(jegyKartya(j));
}

/** Kártya a listában (mint a napi beosztásban): megnyitás teljes képernyőn, illetve visszamondás. */
function jegyKartya(j) {
    const kartya = elem("article", "kartya jegy-kartya" + (j.ervenyes ? "" : " elhalvanyitott"));

    if (j.logoUrl) {
        const logo = elem("img", "jegy-kartya-logo");
        logo.src = j.logoUrl;
        logo.alt = "";
        kartya.append(logo);
    }
    const tartalom = elem("div", "film-tartalom");
    const fej = elem("div", "kartya-fej");
    fej.append(elem("h2", "", j.filmCim), elem("span", "idopont", ido(j.kezdes) + " – " + ido(j.befejezes)));
    const cimkek = elem("div", "cimkek");
    cimkek.append(
        elem("span", "cimke kiemelt", vetitesTipus(j.vetitesTipus)),
        elem("span", "cimke", vetitesNyelv(j.eredetiNyelvu)),
        elem("span", "cimke" + (j.ervenyes ? "" : " lejart-cimke"), j.ervenyes ? "Érvényes" : "Lejárt"));
    tartalom.append(fej, cimkek, adatlista([
        ["Nap", szepDatum(j.datum)],
        ["Terem", j.teremNev + " · " + j.szekSzam + ". szék"],
        ["Jegy", j.jegyNev + " – " + forint(j.ar)],
        ["Foglalási kód", j.foglalasKod],
    ]));

    const gombok = elem("div", "gombsor terkoz-felul");
    const nyit = elem("a", "gomb kicsi", "Jegy megnyitása");
    nyit.href = "jegyeim.html?kod=" + encodeURIComponent(j.foglalasKod);
    nyit.addEventListener("click", e => {
        e.preventDefault();
        megnyit(j);
    });
    gombok.append(nyit);
    if (j.lemondhato) {
        const lemond = elem("button", "gomb veszely kicsi", "Visszamondás");
        lemond.type = "button";
        lemond.addEventListener("click", () => visszamond(j, lemond));
        gombok.append(lemond);
    }
    tartalom.append(gombok);
    if (j.ervenyes && !j.lemondhato) {
        tartalom.append(elem("p", "tipp", "Az előadás már elkezdődött, a jegy nem mondható vissza."));
    }
    kartya.append(tartalom);
    return kartya;
}

// ---------- Visszamondás ----------
/** Egy jegy visszamondása megerősítés után; a szék újra foglalhatóvá válik. */
async function visszamond(j, gomb) {
    const kerdes = "Biztosan visszamondod ezt a jegyet?\n\n" + j.filmCim + "\n" + szepDatum(j.datum) + ", "
        + ido(j.kezdes) + " · " + j.teremNev + " · " + j.szekSzam + ". szék\n" + j.jegyNev + " – " + forint(j.ar)
        + "\n\nA visszamondás nem vonható vissza.";
    if (!confirm(kerdes)) return;
    gomb.disabled = true;
    try {
        await api("/api/jegyfoglalasok/" + encodeURIComponent(j.foglalasKod), { method: "DELETE" });
        bezar();
        jegyek = jegyek.filter(x => x.foglalasKod !== j.foglalasKod);
        megjelenit();
        uzenet(doboz, "Visszamondtad a jegyet: " + j.filmCim + ", " + szepDatum(j.datum) + " " + ido(j.kezdes)
            + " (" + j.foglalasKod + ").", "siker");
    } catch (h) {
        // A teljes képernyős nézet eltakarná a hibaüzenetet.
        bezar();
        uzenet(doboz, h.message);
        gomb.disabled = false;
        doboz.scrollIntoView({ block: "nearest" });
    }
}

// Visszamondás foglalási kóddal (pl. ha a jegy nem jelenik meg a listában).
const kodUrlap = document.getElementById("kod-urlap");
kodUrlap.addEventListener("submit", async e => {
    e.preventDefault();
    const kod = kodUrlap.foglalasKod.value.trim().toUpperCase();
    if (!kod) {
        uzenet(doboz, "Add meg a foglalási kódot.");
        return;
    }
    const meglevo = jegyek.find(x => x.foglalasKod === kod);
    if (meglevo) {
        if (!meglevo.lemondhato) {
            uzenet(doboz, "Ez az előadás már elkezdődött vagy lezajlott, a foglalás nem mondható vissza.");
            return;
        }
        await visszamond(meglevo, kodUrlap.querySelector("button"));
        if (!jegyek.includes(meglevo)) kodUrlap.reset();
        return;
    }
    // Nincs a betöltött listában: a szerver dönt (nem saját vagy nem létező kód esetén 404).
    if (!confirm("Biztosan visszamondod a(z) " + kod + " foglalást?\nA visszamondás nem vonható vissza.")) return;
    const gomb = kodUrlap.querySelector("button");
    gomb.disabled = true;
    try {
        await api("/api/jegyfoglalasok/" + encodeURIComponent(kod), { method: "DELETE" });
        kodUrlap.reset();
        uzenet(doboz, "Visszamondtad a(z) " + kod + " foglalást.", "siker");
        jegyek = await api("/api/jegyfoglalasok/jegyek");
        megjelenit();
    } catch (h) {
        uzenet(doboz, h.message);
    } finally {
        gomb.disabled = false;
    }
});

// ---------- Teljes képernyős jegy ----------
const nezetLemondas = document.getElementById("nezet-lemondas");
let megnyitott = null;

nezetLemondas.addEventListener("click", () => {
    if (megnyitott) visszamond(megnyitott, nezetLemondas);
});

function megnyit(j) {
    megnyitott = j;
    jegyHelye.replaceChildren(mozijegy(j));
    nezetLemondas.hidden = !j.lemondhato;
    nezetLemondas.disabled = false;
    nezet.hidden = false;
    document.body.classList.add("nincs-gorgetes");
    history.replaceState(null, "", "?nezet=" + szuro.value + "&kod=" + encodeURIComponent(j.foglalasKod));
    // Valódi teljes képernyő, ha a böngésző engedi (kattintásra igen, oldalbetöltéskor nem).
    if (nezet.requestFullscreen && !document.fullscreenElement) nezet.requestFullscreen().catch(() => {});
    document.getElementById("bezar").focus();
}

function bezar() {
    if (nezet.hidden) return;
    megnyitott = null;
    nezet.hidden = true;
    document.body.classList.remove("nincs-gorgetes");
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    history.replaceState(null, "", "?nezet=" + szuro.value);
}

document.getElementById("bezar").addEventListener("click", bezar);
document.addEventListener("keydown", e => { if (e.key === "Escape") bezar(); });
// A háttérre kattintás is bezár.
nezet.addEventListener("click", e => { if (e.target === nezet || e.target === jegyHelye) bezar(); });

/** A mozijegy: bal felső sarokban a film logóplakátja, alatta minden adat. */
function mozijegy(j) {
    const jegy = elem("article", "mozijegy" + (j.ervenyes ? "" : " lejart-jegy"));

    const fej = elem("header", "mozijegy-fej");
    const logo = elem("img", "mozijegy-logo");
    logo.src = j.logoUrl || j.kepUrl || "";
    logo.alt = j.filmCim + " logó";
    const cim = elem("div", "mozijegy-cim");
    cim.append(
        elem("p", "mozijegy-mozi", "🎬 Dynamic Cinema · Mozijegy"),
        elem("h2", "", j.filmCim),
        elem("p", "mozijegy-alcim", [j.mufaj, j.hosszPerc + " perc", vetitesTipus(j.vetitesTipus),
            vetitesNyelv(j.eredetiNyelvu)].join(" · ")));
    const allapot = elem("span", "mozijegy-allapot" + (j.ervenyes ? "" : " lejart"), j.ervenyes ? "ÉRVÉNYES" : "LEJÁRT");
    fej.append(logo, cim, allapot);

    const fo = elem("div", "mozijegy-fo");
    for (const [cimke, ertek] of [
        ["Dátum", szepDatum(j.datum)],
        ["Időpont", ido(j.kezdes) + " – " + ido(j.befejezes)],
        ["Terem", j.teremNev + (j.emelet === 0 ? " (földszint)" : " (" + j.emelet + ". emelet)")],
        ["Szék", j.szekSzam + "."],
    ]) {
        const doboz = elem("div", "mozijegy-mezo");
        doboz.append(elem("span", "", cimke), elem("strong", "", ertek));
        fo.append(doboz);
    }

    const film = [
        ["Rendező", j.rendezo],
        ["Főszereplő", j.foszereplo],
    ];
    if (j.foszinkronszinesz) film.push(["Magyar hang", j.foszinkronszinesz]);
    film.push(
        ["Gyártó ország", j.gyartoOrszag],
        ["Jegytípus", j.jegyNev],
        ["Ár", forint(j.ar)],
        ["Név", j.felhasznaloNev],
        ["Foglalva", new Date(j.foglalva).toLocaleString("hu-HU", { dateStyle: "medium", timeStyle: "short" })],
    );

    const lab = elem("footer", "mozijegy-lab");
    lab.append(elem("span", "", "Foglalási kód"), elem("strong", "mozijegy-kod", j.foglalasKod),
        elem("small", "", "A belépéskor mutasd fel ezt a jegyet a jegyszedőnek."));

    jegy.append(fej, fo, adatlista(film), elem("div", "mozijegy-perforacio"), lab);
    return jegy;
}
