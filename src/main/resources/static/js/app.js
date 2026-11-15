// Közös segédfüggvények: token kezelése, REST-hívások, fejléc, formázás.
// A HTML-űrlapok adatait JSON-ként küldjük a Spring REST API-nak (a JWT tokent az Authorization fejléc viszi).

const TOKEN_KULCS = "mozi.token";
const LEJARAT_KULCS = "mozi.lejarat";
const NEV_KULCS = "mozi.nev";

function token() {
    const lejarat = Number(localStorage.getItem(LEJARAT_KULCS) || 0);
    if (lejarat && Date.now() > lejarat) {
        kijelentkezes(false);
        return null;
    }
    return localStorage.getItem(TOKEN_KULCS);
}

function bejelentkezve() {
    return token() !== null;
}

function tokenMentese(valasz) {
    localStorage.setItem(TOKEN_KULCS, valasz.token);
    localStorage.setItem(LEJARAT_KULCS, String(Date.now() + valasz.lejaratMasodperc * 1000));
}

function kijelentkezes(atiranyit = true) {
    localStorage.removeItem(TOKEN_KULCS);
    localStorage.removeItem(LEJARAT_KULCS);
    localStorage.removeItem(NEV_KULCS);
    if (atiranyit) location.href = "index.html";
}

/** REST-hívás. Hiba esetén a szerver ProblemDetail-üzenetét tartalmazó Error-t dob. */
async function api(url, { method = "GET", body } = {}) {
    const fejlecek = { "Accept": "application/json" };
    if (body !== undefined) fejlecek["Content-Type"] = "application/json";
    const t = token();
    if (t) fejlecek["Authorization"] = "Bearer " + t;

    let valasz;
    try {
        valasz = await fetch(url, { method, headers: fejlecek, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch {
        throw new Error("A szerver nem érhető el.");
    }

    if (valasz.status === 401 && t) {
        kijelentkezes(false);
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
