fejlec("");
const urlap = document.getElementById("regisztracio");
const doboz = document.getElementById("uzenet");
urlap.szuletesiDatum.max = isoDatum(new Date(Date.now() - 86400000));

urlap.addEventListener("submit", async e => {
    e.preventDefault();
    if (urlap.jelszo.value !== urlap.jelszo2.value) {
        uzenet(doboz, "A két jelszó nem egyezik.");
        return;
    }
    const gomb = urlap.querySelector("button");
    gomb.disabled = true;
    try {
        const felhasznalo = await api("/auth/regisztracio", {
            method: "POST",
            body: {
                nev: urlap.nev.value.trim(),
                email: urlap.email.value.trim(),
                jelszo: urlap.jelszo.value,
                szuletesiDatum: urlap.szuletesiDatum.value,
            },
        });
        await api("/auth/bejelentkezes", {
            method: "POST",
            body: { email: urlap.email.value.trim(), jelszo: urlap.jelszo.value },
        });
        localStorage.setItem(NEV_KULCS, felhasznalo.nev);
        location.href = "beosztas.html";
    } catch (hiba) {
        uzenet(doboz, hiba.message);
        gomb.disabled = false;
    }
});
