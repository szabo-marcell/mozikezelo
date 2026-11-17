fejlec("foglalas.html");

const urlap = document.getElementById("foglalas");
const doboz = document.getElementById("uzenet");
const fizetesResz = document.getElementById("fizetes");
const keszResz = document.getElementById("kesz");
let eloadasId = parameter("eloadas");
const folytatando = parameter("rendeles");

let jegyek = [];
let kitoltott = null;
let rendelesId = null;

if (bejelentkezesSzukseges()) {
    const indulas = folytatando ? rendelesFolytatasa(folytatando)
        : eloadasId ? kivalasztottEloadas(eloadasId) : uresUrlap();
    indulas.catch(h => uzenet(doboz, h.message));
}

/** "Félbehagyott rendeléseim" → Folytatás: az előadás, a jegytípus és a szék ki van töltve. */
async function rendelesFolytatasa(id) {
    const r = await api("/api/felbehagyott-rendelesek/" + encodeURIComponent(id));
    rendelesId = r.id;
    eloadasId = String(r.napiBeosztasId);
    await kivalasztottEloadas(eloadasId);
    document.getElementById("alcim").textContent =
        "Félbehagyott rendelésed folytatása – ellenőrizd az adatokat, majd lépj tovább a fizetéshez.";
    urlap.jegyNev.value = r.jegyNev;
    urlap.jegyNev.dispatchEvent(new Event("change"));
    if ([...urlap.szekSzam.options].some(o => o.value === String(r.szekSzam))) {
        urlap.szekSzam.value = String(r.szekSzam);
    } else if (r.folytathato === false) {
        uzenet(doboz, "A korábban választott " + r.szekSzam + ". szék közben elkelt, válassz másikat.", "info");
    }
}

/** A "Tovább a fizetéshez" lépésnél félbehagyott rendelésként mentjük az adatokat. */
async function rendelesMentese(adatok) {
    const body = { ...adatok, teremNev: adatok.teremNev || null };
    if (rendelesId) {
        try {
            return await api("/api/felbehagyott-rendelesek/" + rendelesId, { method: "PUT", body });
        } catch {
        }
    }
    return api("/api/felbehagyott-rendelesek", { method: "POST", body });
}

/** Napi beosztásból érkezve: film, dátum, kezdés, terem kitöltve; jegytípus és szék még üres. */
async function kivalasztottEloadas(id) {
    const r = await api("/api/napi-beosztas/" + encodeURIComponent(id));
    const v = r.eloadas;
    document.getElementById("alcim").textContent =
        "A kiválasztott vetítés adatai ki vannak töltve – válassz jegytípust és széket.";

    for (const [mezo, ertek] of [["filmCim", v.filmCim], ["datum", v.datum], ["kezdes", ido(v.kezdes)], ["teremNev", v.teremNev]]) {
        urlap[mezo].value = ertek;
        urlap[mezo].readOnly = true;
        urlap[mezo].removeAttribute("list");
    }
    document.getElementById("terem-tipp").textContent =
        vetitesTipus(v.vetitesTipus) + " · " + (v.eredetiNyelvu ? "eredeti nyelvű" : "szinkronizált") +
        " · vége: " + ido(v.befejezes);
    document.getElementById("vissza-link").href = "beosztas.html?datum=" + v.datum;

    jegyekBetoltese(r.valaszthatoJegyek);
    szabadSzekek(r.szabadSzekek, r.ferohely);

    if (!r.foglalhato) {
        uzenet(doboz, r.szabadSzekek.length ? "Erre a vetítésre már nem lehet foglalni, mert elkezdődött."
            : "Erre a vetítésre minden hely elkelt.", "info");
        urlap.querySelector("button[type=submit]").disabled = true;
    }
}

/** Menüből érkezve: üres űrlap, a filmekhez és termekhez javaslatlistával. */
async function uresUrlap() {
    urlap.datum.min = isoDatum(new Date());
    const [filmek, termek, osszesJegy] = await Promise.all([api("/api/filmek"), api("/api/termek"), api("/api/jegyek")]);
    for (const f of filmek) document.getElementById("filmlista").append(Object.assign(elem("option"), { value: f.cim }));
    for (const t of termek) document.getElementById("teremlista").append(Object.assign(elem("option"), { value: t.nev }));
    jegyekBetoltese(osszesJegy);
}

function jegyekBetoltese(lista) {
    jegyek = lista;
    const select = urlap.jegyNev;
    select.length = 1;
    for (const j of lista) {
        const opcio = elem("option", "", j.nev + " – " + forint(j.ar));
        opcio.value = j.nev;
        select.append(opcio);
    }
}

urlap.jegyNev.addEventListener("change", () => {
    const j = jegyek.find(x => x.nev === urlap.jegyNev.value);
    document.getElementById("jegy-feltetel").textContent = j ? "Feltétel: " + j.feltetel : "";
});

/** A szék mezőt a szabad székek listájára cseréljük. */
function szabadSzekek(szabad, ferohely) {
    const regi = urlap.szekSzam;
    const select = elem("select");
    select.id = select.name = "szekSzam";
    select.required = true;
    select.append(Object.assign(elem("option", "", "– Válassz széket (" + szabad.length + "/" + ferohely + " szabad) –"), { value: "" }));
    for (const sz of szabad) select.append(Object.assign(elem("option", "", sz + ". szék"), { value: sz }));
    regi.replaceWith(select);
}

urlap.addEventListener("submit", async e => {
    e.preventDefault();
    if (!urlap.reportValidity()) return;
    const jegy = jegyek.find(j => j.nev === urlap.jegyNev.value);
    kitoltott = {
        filmCim: urlap.filmCim.value.trim(),
        datum: urlap.datum.value,
        kezdes: urlap.kezdes.value,
        teremNev: urlap.teremNev.value.trim(),
        jegyNev: jegy.nev,
        szekSzam: Number(urlap.szekSzam.value),
    };
    const gomb = urlap.querySelector("button[type=submit]");
    gomb.disabled = true;
    try {
        rendelesId = (await rendelesMentese(kitoltott)).id;
    } catch (h) {
        uzenet(doboz, h.message);
        return;
    } finally {
        gomb.disabled = false;
    }
    const osszesito = document.getElementById("osszesito");
    osszesito.replaceChildren(adatlista([
        ["Film", kitoltott.filmCim],
        ["Időpont", szepDatum(kitoltott.datum) + ", " + kitoltott.kezdes],
        ["Terem", kitoltott.teremNev || "–"],
        ["Jegytípus", jegy.nev],
        ["Szék", kitoltott.szekSzam + ". szék"],
    ]));
    document.getElementById("osszeg").textContent = forint(jegy.ar);
    uzenet(doboz, "");
    urlap.hidden = true;
    fizetesResz.hidden = false;
});

document.getElementById("modosit").addEventListener("click", () => {
    fizetesResz.hidden = true;
    urlap.hidden = false;
});

document.getElementById("fizet").addEventListener("click", async () => {
    const gomb = document.getElementById("fizet");
    gomb.disabled = true;
    gomb.textContent = "Fizetés folyamatban…";
    try {
        await new Promise(r => setTimeout(r, 800));
        const foglalas = eloadasId
            ? await api("/api/napi-beosztas/" + encodeURIComponent(eloadasId) + "/foglalas", {
                method: "POST",
                body: { jegyNev: kitoltott.jegyNev, szekSzam: kitoltott.szekSzam },
            })
            : await api("/api/jegyfoglalasok", {
                method: "POST",
                body: { ...kitoltott, teremNev: kitoltott.teremNev || null },
            });
        visszaigazolas(foglalas);
    } catch (h) {
        uzenet(doboz, h.message + " A fizetés nem történt meg.");
        fizetesResz.hidden = true;
        urlap.hidden = false;
        if (eloadasId) {
            const r = await api("/api/napi-beosztas/" + encodeURIComponent(eloadasId)).catch(() => null);
            if (r) szabadSzekek(r.szabadSzekek, r.ferohely);
        }
    } finally {
        gomb.disabled = false;
        gomb.textContent = "Fizetés";
    }
});

function visszaigazolas(f) {
    rendelesId = null;
    document.getElementById("kod").textContent = f.foglalasKod;
    document.getElementById("jegy-link").href = "jegyeim.html?kod=" + encodeURIComponent(f.foglalasKod);
    document.getElementById("visszaigazolas").replaceChildren(adatlista([
        ["Film", f.filmCim],
        ["Időpont", szepDatum(f.datum) + ", " + ido(f.kezdes) + " – " + ido(f.befejezes)],
        ["Terem", f.teremNev],
        ["Jegytípus", f.jegyNev],
        ["Szék", f.szekSzam + ". szék"],
        ["Fizetve", forint(f.ar)],
    ]));
    uzenet(doboz, "");
    fizetesResz.hidden = true;
    keszResz.hidden = false;
}
