// bejelentkezes.html oldal szkriptje (a CSP miatt nincs inline JavaScript).
fejlec("");
const urlap = document.getElementById("bejelentkezes");
const doboz = document.getElementById("uzenet");
// A foglalási oldal ide irányít, ha nincs bejelentkezve; sikeres belépés után oda térünk vissza.
const vissza = parameter("vissza");
if (vissza) uzenet(doboz, "A helyfoglaláshoz jelentkezz be.", "info");

urlap.addEventListener("submit", async e => {
    e.preventDefault();
    const gomb = urlap.querySelector("button");
    gomb.disabled = true;
    try {
        // A token HttpOnly sütiben érkezik, a böngésző kezeli.
        await api("/auth/bejelentkezes", {
            method: "POST",
            body: { email: urlap.email.value.trim(), jelszo: urlap.jelszo.value },
        });
        const profil = await api("/api/felhasznalo/profil");
        localStorage.setItem(NEV_KULCS, profil.nev);
        localStorage.setItem(JOG_KULCS, profil.jogNev || "");
        // Csak saját oldalra irányítunk vissza.
        location.href = vissza && /^[a-z]+\.html(\?[\w=&%-]*)?$/.test(vissza) ? vissza : "beosztas.html";
    } catch (hiba) {
        uzenet(doboz, hiba.message);
        gomb.disabled = false;
    }
});
