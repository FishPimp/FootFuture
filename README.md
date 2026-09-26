# Storleksprognosen 🌱

En lekfull Android-app som förutser barnens kropps- och fotlängd, räknar ut vilka
kläd- och skostorlekar som behövs kommande säsonger och matchar sparade plagg mot
syskonens prognoser.

Byggd i **Kotlin** med **Jetpack Compose**, **Material 3** och **Room**, enligt samma
upplägg som PackMaster (ren Kotlin-domänmodul + Android-app, MVVM). Hela
gränssnittet är på svenska och alla texter ligger i `res/values/strings.xml`.

## Flikar

| Flik | Innehåll |
| --- | --- |
| 📊 **Prognosen** | Tillväxtdiagram i Compose `Canvas` för 0–72 månader med percentilkurvorna P3, P15, P50, P85 och P97, säsongsskuggning (❄️ blått för vinter, ☀️ gult för sommar), mätpunkter och en streckad prognoslinje. Växla mellan längd (cm) och fot (mm). Dra i åldersslidern – eller direkt i grafen – så studsar en ruta fram med rekommenderad klädstorlek och skostorlek och hur länge den räcker. Här hanteras även barn och mätningar. |
| 📦 **Arvsmatrisen** | Förrådet med sparade plagg som `ElevatedCard`s. Varje plagg jämförs med syskonens prognos: 🟢 *Passar Lillasyster vintern 2026/27 (Stl 24)*, 🔴 *Spara ej – Lillasyster når stl 24 i juli 2027*. Säsongsmatrisen visar vilken storlek varje barn behöver kommande säsonger och flaggar ⚪ luckor: *Inget sparat för Vinter 2026/27 (Behöver Stl 26)*. |
| 📏 **Fotmätaren** | Kalibrera skärmen med ett kreditkort (85,6 mm) och mät foten med väggmetoden: mobilen plant mot väggen, hälen mot väggen och en dragbar skjutmåttslinje till stortåns spets. Mätningen sparas direkt på barnet. |

Knappen **Visa exempel** på startsidan fyller appen med två syskon, mätningar och
några plagg, så att allt går att prova direkt.

## Beräkningar

- **Kroppslängd:** WHO:s LMS-tabeller för flickor och pojkar – WHO Child Growth
  Standards 0–60 mån (liggande längd till 24 mån, därefter stående) och WHO Growth
  Reference 2007 för 61–72 mån. Z = ((X/M)^L − 1)/(L·S).
- **Fotlängd:** Det finns ingen officiell WHO- eller BVC-kurva, så fotlängden härleds ur
  längden: median ≈ 15,6–15,8 % av medianlängden och en variationskoefficient som
  kombinerar längdens spridning med kvotens (≈ 5,5–6 % totalt). Kurvorna märks som
  uppskattning i appen.
- **Prognos:** Barnet antas följa sin percentilkanal (konstant Z-score) från den
  senaste mätningen. Saknas fotmätning följer foten längdens kanal.
- **Klädstorlek (centilong):** minsta storlek i 6 cm-steg (44, 50, … 152) som är minst
  lika lång som barnet.
- **Skostorlek (EU):** (fotlängd + växtmån) ÷ 6,67 mm, avrundat uppåt. Växtmånen är
  12 mm som standard och kan ändras (8–20 mm) i Fotmätaren.
- **Säsonger:** vinter nov–mar, vår apr–maj, sommar jun–aug, höst sep–okt. Ett plagg
  ska räcka hela säsongen, så storleken som behövs är prognosens storlek på
  säsongens sista dag.

## Arkitektur

```
domain/   Ren Kotlin-modul: modeller, WHO-referens (LMS), fotreferens, prognos,
          storlekar, säsongskalender, arvsmatchning och repository-gränssnitt
app/
  data/   Room (entities, DAO:er, databas), repositories, inställningar, exempeldata
  ui/     Compose-skärmar + ViewModels (Prognosen, Arvsmatrisen, Fotmätaren)
```

Datamodellen följer specen (`children`, `measurements`, `inventory`), med två
tillägg: fot- och kroppslängd är valfria var för sig (fotmätaren sparar bara foten)
och mätningar och plagg tas bort tillsammans med barnet (främmande nycklar).

Domänlogiken (Z-score mot WHO:s publicerade SD-värden, fotreferens, storlekar,
säsonger, prognos och matchning) är enhetstestad i `domain/src/test`.

## Typsnitt

Rubriker i **Fredoka** och brödtext i **Plus Jakarta Sans**, inbäddade som statiska
vikter i `res/font`. Båda är licensierade under SIL Open Font License 1.1 – se
`licenses/`.

## Bygga

Kräver JDK 17 och Android SDK (compileSdk 35).

```bash
./gradlew :domain:test          # enhetstester
./gradlew :app:assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

`.github/workflows/build.yml` kör testerna och bygger `app-debug.apk` vid varje push
till `main` (samt för pull requests och manuellt). APK:n laddas upp som
workflow-artefakten **app-debug**.
