fejlec("index.html");
api("/api/mozi").then(m => {
    document.getElementById("mozi-nev").textContent = m.nev;
    document.getElementById("mozi-cim").textContent = m.varos + ", " + m.utca + " · épült: " + m.epitesEve;
    document.title = m.nev + " – Kezdőlap";
}).catch(() => {});
