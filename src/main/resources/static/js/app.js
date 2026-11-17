// Közös segédfüggvények: bejelentkezési állapot, REST-hívások, fejléc, formázás.
// A HTML-űrlapok adatait JSON-ként küldjük a Spring REST API-nak.
// A JWT a szerver által beállított HttpOnly sütiben (MOZI_TOKEN) utazik: JavaScriptből nem olvasható,
// ezért itt nem is tároljuk. A módosító kérésekhez CSRF-token kell (XSRF-TOKEN süti -> X-XSRF-TOKEN fejléc).

// Csak a fejléc megjelenítéséhez; a jogosultságot a szerver minden kérésnél ellenőrzi.
const NEV_KULCS = "mozi.nev";
const JOG_KULCS = "mozi.jog";
// Titkot nem tartalmazó jelzősüti, a tokennel együtt jár le.
const JELZO_SUTI = "MOZI_BEJELENTKEZVE";
const CSRF_SUTI = "XSRF-TOKEN";

function suti(nev) {
    const talalat = document.cookie.split("; ").find(s => s.startsWith(nev + "="));
    return talalat ? decodeURIComponent(talalat.slice(nev.length + 1)) : null;
}

function admin() {
    return bejelentkezve() && localStorage.getItem(JOG_KULCS) === "ADMIN";
}

function bejelentkezve() {
    if (suti(JELZO_SUTI) === "1") return true;
    // Lejárt vagy visszavont munkamenet: a megjelenítési adatok is mennek.
    helyiAdatokTorlese();
    return false;
}

function helyiAdatokTorlese() {
    localStorage.removeItem(NEV_KULCS);
    localStorage.removeItem(JOG_KULCS);
    // Régebbi verzió maradványai: a token már nem a localStorage-ban van.
    localStorage.removeItem("mozi.token");
    localStorage.removeItem("mozi.lejarat");
}

/** Kijelentkezés: a szerver visszavonja a tokeneket és törli a sütiket. */
async function kijelentkezes(atiranyit = true) {
    try {
        await api("/auth/kijelentkezes", { method: "POST" });
    } catch {
        // A helyi adatokat akkor is töröljük, ha a szerver nem érhető el.
    }
    helyiAdatokTorlese();
    if (atiranyit) location.href = "index.html";
}

async function csrfToken() {
    let t = suti(CSRF_SUTI);
    if (!t) {
        await fetch("/auth/csrf", { credentials: "same-origin" });
        t = suti(CSRF_SUTI);
    }
    return t;
}

/** REST-hívás. Hiba esetén a szerver ProblemDetail-üzenetét tartalmazó Error-t dob. */
async function api(url, { method = "GET", body } = {}, ujraprobalva = false) {
    const fejlecek = { "Accept": "application/json" };
    if (body !== undefined) fejlecek["Content-Type"] = "application/json";
    const modosito = !["GET", "HEAD", "OPTIONS"].includes(method);
    const voltBejelentkezve = bejelentkezve();

    let valasz;
    try {
        if (modosito) {
            const t = await csrfToken();
            if (t) fejlecek["X-XSRF-TOKEN"] = t;
        }
        valasz = await fetch(url, {
            method, headers: fejlecek, credentials: "same-origin",
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch {
        throw new Error("A szerver nem érhető el.");
    }

    // Hiányzó vagy elavult CSRF-token (pl. törölt süti): egyszer újrapróbáljuk friss tokennel.
    // (Valódi jogosultsági hiba esetén az újrapróbálás is 403-at ad, és az jut el a hívóhoz.)
    if (valasz.status === 403 && modosito && !ujraprobalva) {
        await fetch("/auth/csrf", { credentials: "same-origin" });
        return api(url, { method, body }, true);
    }

    if (valasz.status === 401 && voltBejelentkezve) {
        helyiAdatokTorlese();
        throw new Error("A munkamenet lejárt, jelentkezz be újra.");
    }
    if (valasz.status === 204) return null;

    const szoveg = await valasz.text();
    const adat = szoveg ? JSON.parse(szoveg) : null;
    if (!valasz.ok) throw new Error(hibauzenet(adat, valasz.status));
    return adat;
}

const MEZONEVEK = {
    nev: "Név", email: "E-mail", jelszo: "Jelszó", szuletesiDatum: "Születési dátum",
    filmCim: "Film", datum: "Dátum", kezdes: "Kezdés", teremNev: "Terem", jegyNev: "Jegytípus", szekSzam: "Szék",
};

function hibauzenet(adat, statusz) {
    if (!adat) return "Hiba történt (" + statusz + ").";
    // Validációs hibák mezőnként (ApiExceptionHandler: errors = [{field, message}]).
    if (Array.isArray(adat.errors) && adat.errors.length) {
        return adat.errors.map(h => (h.field ? (MEZONEVEK[h.field] || h.field) + ": " : "") + h.message).join("; ");
    }
    return adat.detail || adat.title || "Hiba történt (" + statusz + ").";
}

// ---------- Fejléc ----------
const ADMIN_MENUPONTOK = [["admin.html", "Filmek kezelése"], ["musor.html", "Műsor kezelése"]];

function fejlec(aktiv) {
    const menupontok = [
        ["index.html", "Kezdőlap"],
        ["filmek.html", "Filmek"],
        ["beosztas.html", "Napi beosztás"],
        ["foglalas.html", "Helyfoglalás"],
    ];
    // A saját jegyek és a félbehagyott rendelések csak bejelentkezve látszanak.
    if (bejelentkezve()) {
        menupontok.push(["jegyeim.html", "Jegyeim"], ["felbehagyott.html", "Félbehagyott rendeléseim"]);
    }
    if (admin()) menupontok.push(...ADMIN_MENUPONTOK);
    const header = document.createElement("header");
    header.className = "fejlec";
    const belso = elem("div", "fejlec-belso");
    const logo = elem("a", "logo", "🎬 Dynamic Cinema");
    logo.href = "index.html";
    const menu = elem("nav", "menu");
    for (const [href, felirat] of menupontok) {
        const a = elem("a", href === aktiv ? "aktiv" : "", felirat);
        a.href = href;
        menu.append(a);
    }
    const fiok = elem("div", "fiok");
    if (bejelentkezve()) {
        fiok.append(elem("span", "", localStorage.getItem(NEV_KULCS) || "Bejelentkezve"));
        const ki = elem("button", "gomb masodlagos kicsi", "Kijelentkezés");
        ki.type = "button";
        ki.addEventListener("click", () => kijelentkezes());
        fiok.append(ki);
    } else {
        const be = elem("a", "gomb masodlagos kicsi", "Bejelentkezés");
        be.href = "bejelentkezes.html";
        const reg = elem("a", "gomb kicsi", "Regisztráció");
        reg.href = "regisztracio.html";
        fiok.append(be, reg);
    }
    belso.append(logo, menu, fiok);
    header.append(belso);
    document.body.prepend(header);

    // Korábbi bejelentkezésnél még nincs eltárolva a jog: lekérjük, és adminnak pótoljuk a menüpontot.
    if (bejelentkezve() && localStorage.getItem(JOG_KULCS) === null) {
        api("/api/felhasznalo/profil").then(p => {
            localStorage.setItem(JOG_KULCS, p.jogNev || "");
            if (admin()) {
                for (const [href, felirat] of ADMIN_MENUPONTOK) {
                    const a = elem("a", aktiv === href ? "aktiv" : "", felirat);
                    a.href = href;
                    menu.append(a);
                }
            }
        }).catch(() => {});
    }
}

// ---------- DOM és formázás ----------
/** Elem létrehozása; a szöveg textContent-ként kerül be (nincs HTML-befecskendezés). */
function elem(tag, osztaly = "", szoveg) {
    const e = document.createElement(tag);
    if (osztaly) e.className = osztaly;
    if (szoveg !== undefined && szoveg !== null) e.textContent = szoveg;
    return e;
}

/** Definíciós lista (dt/dd párok) a kártyákhoz. */
function adatlista(parok) {
    const dl = elem("dl", "adatok");
    for (const [kulcs, ertek] of parok) {
        dl.append(elem("dt", "", kulcs), elem("dd", "", ertek));
    }
    return dl;
}

function uzenet(cel, szoveg, tipus = "hiba") {
    cel.className = "uzenet " + tipus;
    cel.textContent = szoveg || "";
}

const ido = t => (t || "").slice(0, 5);
const forint = n => new Intl.NumberFormat("hu-HU").format(n) + " Ft";
const vetitesTipus = t => (t === "HAROM_D" ? "3D" : t === "KET_D" ? "2D" : t);

/** Helyi dátum ISO-formátumban (YYYY-MM-DD), időzóna-eltolás nélkül. */
function isoDatum(d) {
    const ho = String(d.getMonth() + 1).padStart(2, "0");
    const nap = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + ho + "-" + nap;
}

function szepDatum(iso) {
    const [e, h, n] = iso.split("-").map(Number);
    return new Date(e, h - 1, n).toLocaleDateString("hu-HU", { year: "numeric", month: "long", day: "numeric", weekday: "long" });
}

/** Bejelentkezés nélkül a bejelentkezési oldalra irányít, majd onnan vissza ide. Igazat ad, ha be van jelentkezve. */
function bejelentkezesSzukseges() {
    if (bejelentkezve()) return true;
    const oldal = location.pathname.split("/").pop() || "index.html";
    location.replace("bejelentkezes.html?vissza=" + encodeURIComponent(oldal + location.search));
    return false;
}

const vetitesNyelv = eredeti => (eredeti ? "Eredeti nyelvű (feliratos)" : "Szinkronizált");

function parameter(nev) {
    return new URLSearchParams(location.search).get(nev);
}
