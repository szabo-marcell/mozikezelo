fejlec("filmek.html");
const lista = document.getElementById("filmek");

api("/api/filmek").then(filmek => {
    if (!filmek.length) {
        lista.append(elem("p", "ures", "Jelenleg nincs film a műsoron."));
        return;
    }
    for (const f of filmek) {
        const kartya = elem("div", "kartya film-kartya");
        if (f.kepUrl) {
            const kep = elem("img", "plakat");
            kep.src = f.kepUrl;
            kep.alt = f.cim + " plakátja";
            kep.loading = "lazy";
            kartya.append(kep);
        }
        const tartalom = elem("div", "film-tartalom");
        const fej = elem("div", "kartya-fej");
        fej.append(elem("h2", "", f.cim), elem("span", "cimke kiemelt", f.hosszPerc + " perc"));
        const cimkek = elem("div", "cimkek");
        cimkek.append(elem("span", "cimke", f.mufaj), elem("span", "cimke", f.gyartoOrszag));
        const adatok = [
            ["Bemutató", szepDatum(f.bemutatoDatum)],
            ["Rendező", f.rendezo],
            ["Főszereplő", f.foszereplo],
        ];
        if (f.foszinkronszinesz) adatok.push(["Magyar hang", f.foszinkronszinesz]);
        adatok.push(["Gyártó", f.gyartoCeg], ["Forgalmazó", f.forgalmazo]);
        tartalom.append(fej, cimkek);
        if (f.leiras) tartalom.append(elem("p", "leiras", f.leiras));
        tartalom.append(adatlista(adatok));
        kartya.append(tartalom);
        lista.append(kartya);
    }
}).catch(h => uzenet(document.getElementById("uzenet"), h.message));
