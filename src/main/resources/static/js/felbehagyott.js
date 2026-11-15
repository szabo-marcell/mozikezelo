// felbehagyott.html oldal szkriptje (a CSP miatt nincs inline JavaScript).
fejlec("felbehagyott.html");

const lista = document.getElementById("rendelesek");
const doboz = document.getElementById("uzenet");

if (bejelentkezesSzukseges()) betolt();

async function betolt() {
    lista.replaceChildren();
    try {
        const rendelesek = await api("/api/felbehagyott-rendelesek");
        if (!rendelesek.length) {
            const p = elem("p", "ures", "Nincs félbehagyott rendelésed.");
            const a = elem("a", "", " Napi beosztás →");
            a.href = "beosztas.html";
            p.append(a);
            lista.append(p);
            return;
        }
        for (const r of rendelesek) lista.append(rendelesKartya(r));
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

function rendelesKartya(r) {
    const kartya = elem("div", "kartya jegy-kartya" + (r.folytathato ? "" : " elhalvanyitott"));
    if (r.logoUrl) {
        const logo = elem("img", "jegy-kartya-logo");
        logo.src = r.logoUrl;
        logo.alt = "";
        kartya.append(logo);
    }
    const tartalom = elem("div", "film-tartalom");
    const fej = elem("div", "kartya-fej");
    fej.append(elem("h2", "", r.filmCim), elem("span", "idopont", ido(r.kezdes) + " – " + ido(r.befejezes)));
    const cimkek = elem("div", "cimkek");
    cimkek.append(
        elem("span", "cimke kiemelt", vetitesTipus(r.vetitesTipus)),
        elem("span", "cimke", vetitesNyelv(r.eredetiNyelvu)),
        elem("span", "cimke" + (r.folytathato ? "" : " lejart-cimke"), r.folytathato ? "Fizetésre vár" : "Nem folytatható"));

    const gombok = elem("div", "gombsor terkoz-felul");
    if (r.folytathato) {
        const tovabb = elem("a", "gomb", "Folytatás →");
        tovabb.href = "foglalas.html?rendeles=" + r.id;
        gombok.append(tovabb);
    }
    const torles = elem("button", "gomb masodlagos", "Törlés");
    torles.type = "button";
    torles.addEventListener("click", async () => {
        if (!confirm("Biztosan törlöd ezt a félbehagyott rendelést (" + r.filmCim + ")?")) return;
        torles.disabled = true;
        try {
            await api("/api/felbehagyott-rendelesek/" + r.id, { method: "DELETE" });
            uzenet(doboz, "A félbehagyott rendelést töröltük.", "siker");
            await betolt();
        } catch (h) {
            uzenet(doboz, h.message);
            torles.disabled = false;
        }
    });
    gombok.append(torles);

    tartalom.append(fej, cimkek, adatlista([
        ["Nap", szepDatum(r.datum)],
        ["Terem", r.teremNev + " · " + r.szekSzam + ". szék"],
        ["Jegy", r.jegyNev + " – " + forint(r.ar)],
        ["Utoljára módosítva", new Date(r.modositva).toLocaleString("hu-HU", { dateStyle: "medium", timeStyle: "short" })],
    ]));
    if (!r.folytathato) {
        tartalom.append(elem("p", "tipp", "Az előadás már elkezdődött, vagy a széket közben más lefoglalta – ez a rendelés csak törölhető."));
    }
    tartalom.append(gombok);
    kartya.append(tartalom);
    return kartya;
}
