fejlec("admin.html");

const doboz = document.getElementById("uzenet");
const listaResz = document.getElementById("lista-resz");
const lista = document.getElementById("filmek");
const kereso = document.getElementById("kereso");
const urlap = document.getElementById("film-urlap");

const MEZOK = ["cim", "bemutatoDatum", "hosszPerc", "mufaj", "gyartoOrszag", "forgalmazo", "gyartoCeg",
    "foszereplo", "rendezo", "foszinkronszinesz", "leiras", "kepUrl", "logoUrl"];
const NEM_KOTELEZO = new Set(["foszinkronszinesz", "leiras", "kepUrl", "logoUrl"]);

let filmek = [];
let szerkesztett = null;

if (bejelentkezesSzukseges()) indit();

async function indit() {
    try {
        const profil = await api("/api/felhasznalo/profil");
        localStorage.setItem(JOG_KULCS, profil.jogNev || "");
        if (profil.jogNev !== "ADMIN") {
            listaResz.hidden = true;
            uzenet(doboz, "Ez az oldal csak adminisztrátoroknak érhető el.");
            return;
        }
        await betolt();
    } catch (h) {
        uzenet(doboz, h.message);
    }
}

async function betolt() {
    filmek = await api("/api/filmek");
    const mufajok = [...new Set(filmek.map(f => f.mufaj))].sort((a, b) => a.localeCompare(b, "hu"));
    document.getElementById("mufajlista").replaceChildren(
        ...mufajok.map(m => Object.assign(elem("option"), { value: m })));
    megjelenit();
}

kereso.addEventListener("input", megjelenit);

function megjelenit() {
    const szo = kereso.value.trim().toLocaleLowerCase("hu");
    const lathato = filmek.filter(f => !szo ||
        [f.cim, f.rendezo, f.mufaj, f.foszereplo].some(x => (x || "").toLocaleLowerCase("hu").includes(szo)));
    lista.replaceChildren();
    if (!lathato.length) {
        lista.append(elem("p", "ures", filmek.length ? "Nincs a keresésnek megfelelő film." : "Még nincs film. Vegyél fel egyet!"));
        return;
    }
    for (const f of lathato) lista.append(filmSor(f));
}

function filmSor(f) {
    const kartya = elem("div", "kartya jegy-kartya");
    const kep = elem("img", "jegy-kartya-logo");
    kep.alt = "";
    if (f.kepUrl || f.logoUrl) {
        kep.src = f.kepUrl || f.logoUrl;
        kartya.append(kep);
    }
    const tartalom = elem("div", "film-tartalom");
    const fej = elem("div", "kartya-fej");
    fej.append(elem("h2", "", f.cim), elem("span", "cimke kiemelt", f.hosszPerc + " perc"));
    const cimkek = elem("div", "cimkek");
    cimkek.append(elem("span", "cimke", f.mufaj), elem("span", "cimke", f.gyartoOrszag),
        elem("span", "cimke", "Bemutató: " + f.bemutatoDatum));
    if (!f.kepUrl) cimkek.append(elem("span", "cimke lejart-cimke", "Nincs plakát"));

    const gombok = elem("div", "gombsor terkoz-felul");
    const szerk = elem("button", "gomb kicsi", "Szerkesztés");
    szerk.type = "button";
    szerk.addEventListener("click", () => urlapMegnyitasa(f));
    const torles = elem("button", "gomb masodlagos kicsi", "Törlés");
    torles.type = "button";
    torles.addEventListener("click", () => torol(f, torles));
    gombok.append(szerk, torles);

    tartalom.append(fej, cimkek, adatlista([
        ["Rendező", f.rendezo],
        ["Főszereplő", f.foszereplo + (f.foszinkronszinesz ? " (magyar hang: " + f.foszinkronszinesz + ")" : "")],
        ["Forgalmazó", f.forgalmazo],
    ]), gombok);
    kartya.append(tartalom);
    return kartya;
}

async function torol(f, gomb) {
    if (!confirm("Biztosan törlöd a(z) \"" + f.cim + "\" filmet?\nA napi beosztásban szereplő vagy lefoglalt film nem törölhető.")) return;
    gomb.disabled = true;
    try {
        await api("/api/filmek/" + f.id, { method: "DELETE" });
        uzenet(doboz, "A(z) \"" + f.cim + "\" filmet töröltük.", "siker");
        await betolt();
    } catch (h) {
        uzenet(doboz, h.message);
        gomb.disabled = false;
    }
}

document.getElementById("uj-film").addEventListener("click", () => urlapMegnyitasa(null));
document.getElementById("megse").addEventListener("click", urlapBezarasa);

function urlapMegnyitasa(f) {
    szerkesztett = f;
    urlap.reset();
    document.getElementById("urlap-cim").textContent = f ? "Film módosítása: " + f.cim : "Új film";
    document.getElementById("mentes").textContent = f ? "Módosítások mentése" : "Film felvétele";
    for (const m of MEZOK) urlap[m].value = f && f[m] != null ? f[m] : "";
    document.getElementById("hossz-tipp").textContent = f
        ? "Ha a film már szerepel a napi beosztásban, a hossza nem módosítható." : "";
    elonezetek();
    szamlalo();
    uzenet(doboz, "");
    listaResz.hidden = true;
    urlap.hidden = false;
    urlap.cim.focus();
}

function urlapBezarasa() {
    urlap.hidden = true;
    listaResz.hidden = false;
    szerkesztett = null;
}

urlap.addEventListener("submit", async e => {
    e.preventDefault();
    if (!urlap.reportValidity()) return;
    const adat = {};
    for (const m of MEZOK) {
        const ertek = urlap[m].value.trim();
        adat[m] = NEM_KOTELEZO.has(m) && !ertek ? null : ertek;
    }
    adat.hosszPerc = Number(adat.hosszPerc);

    const gomb = document.getElementById("mentes");
    const modositas = szerkesztett !== null;
    gomb.disabled = true;
    try {
        const mentett = modositas
            ? await api("/api/filmek/" + szerkesztett.id, { method: "PUT", body: adat })
            : await api("/api/filmek", { method: "POST", body: adat });
        urlapBezarasa();
        await betolt();
        uzenet(doboz, "A(z) \"" + mentett.cim + "\" filmet " + (modositas ? "módosítottuk." : "felvettük."), "siker");
    } catch (h) {
        uzenet(doboz, h.message);
        doboz.scrollIntoView({ behavior: "smooth", block: "center" });
    } finally {
        gomb.disabled = false;
    }
});

function elonezet(mezo, kepId) {
    const kep = document.getElementById(kepId);
    const ut = urlap[mezo].value.trim();
    if (/^\/[^/]/.test(ut)) {
        kep.src = ut;
        kep.hidden = false;
    } else {
        kep.removeAttribute("src");
        kep.hidden = true;
    }
}

function elonezetek() {
    elonezet("kepUrl", "kep-elonezet");
    elonezet("logoUrl", "logo-elonezet");
}

for (const [mezo, kepId] of [["kepUrl", "kep-elonezet"], ["logoUrl", "logo-elonezet"]]) {
    urlap[mezo].addEventListener("input", () => elonezet(mezo, kepId));
    document.getElementById(kepId).addEventListener("error", e => { e.target.hidden = true; });
}

function szamlalo() {
    document.getElementById("leiras-szamlalo").textContent = urlap.leiras.value.length + " / 2000";
}
urlap.leiras.addEventListener("input", szamlalo);
