# Plan: Automatisk oppfølgingsoppgave (VurderKonsekvensForYtelse)

## Problem og tilnærming

Når overgangsstønad stanser i registeret (TOM-datoen er kortere enn våre vedtaksperioder) skal vi **ikke** lenger opprette et oppfølgingsliste-innslag. I stedet skal vi automatisk generere en GSYS-oppgave av typen `VurderKonsekvensForYtelse` som saksbehandler kan plukke fra oppgavelista.

---

## Arkitektur

### Ny sub-pakke: `oppfølging/oppgave/`

```
src/main/kotlin/no/nav/tilleggsstonader/sak/oppfølging/oppgave/
  Oppfølgingsoppgave.kt                    ← Entity (gjenbruker OppfølgingData)
  OppfølgingsoppgaveRepository.kt          ← Spring Data JDBC repo
  OppfølgingsoppgaveService.kt             ← Forretningslogikk + deduplication
  DagligOppfølgingsoppgaveTask.kt          ← Daglig self-scheduling scheduler-task
  OppfølgingsoppgaveBehandlingTask.kt      ← Per-behandling task
  OppfølgingsoppgaveDeteksjonService.kt    ← Detekterer stanset overgangsstønad
```

### Daglig flyt

```
DagligOppfølgingsoppgaveTask.doTask()
  └─ behandlingRepository.finnGjeldendeIverksatteBehandlinger(stønadstype) per Stønadstype
       (samme query som oppfølgingslista – siste iverksatte behandling per fagsak, inkl. OPPHØRT og INNVILGET)
     For hver behandling:
       → opprett OppfølgingsoppgaveBehandlingTask (én per behandling)

  onCompletion() → scheduler ny DagligOppfølgingsoppgaveTask for neste dag kl. 07:00

OppfølgingsoppgaveBehandlingTask.doTask()
  └─ Kall OppfølgingsoppgaveDeteksjonService.detekter(behandlingId)
       → Returnerer `OppfølgingData?` (null = ingen case oppdaget)
       → Hvis ikke null:
            - Hent siste rad fra DB for behandlingId
            - Hvis ingen tidligere rad finnes, ELLER OppfølgingData har endret seg siden sist:
                → Opprett ny GSYS-oppgave
                → Lagre ny Oppfølgingsoppgave-record i DB (med oppgave_id fra den nye oppgaven)
            - Ellers (data uendret): gjør ingenting
       → Ellers (null): gjør ingenting
```

Mønsteret følger `DagligIverksettTask` / `DagligIverksettBehandlingTask` for konsistens.

---

## Datamodell

### Ny tabell: `oppfolgingsoppgave`

```sql
CREATE TABLE oppfolgingsoppgave (
    id              UUID PRIMARY KEY,
    behandling_id   UUID NOT NULL REFERENCES behandling(id),
    oppgave_id      UUID NOT NULL REFERENCES oppgave(id),   -- fremmednøkkel til oppgave-tabellen
    data            JSONB NOT NULL,
    tema            VARCHAR NOT NULL,
    opprettet_tidspunkt TIMESTAMP NOT NULL DEFAULT now()
);
```

### Oppfølgingsoppgave.kt (entity)

Gjenbruk `OppfølgingData` (fra `OppfølgingRepository.kt`) direkte som datatype.
Ingen ny sealed class, ingen ny enum, ingen ny DB-converter nødvendig –
`OppfølgingDataReader`/`Writer` i `DatabaseConfiguration.kt` håndterer dette allerede.

GSYS-informasjon (inkl. `gsakOppgaveId`) hentes via `oppgave`-tabellen gjennom fremmednøkkelen.

```kotlin
@Table("oppfolgingsoppgave")
data class Oppfølgingsoppgave(
    @Id val id: UUID = UUID.randomUUID(),
    val behandlingId: BehandlingId,
    val oppgaveId: UUID,             // FK til oppgave-tabellen
    val data: OppfølgingData,        // gjenbruk fra oppfølgingslista
    val tema: Tema,
    val opprettetTidspunkt: LocalDateTime = SporbarUtils.now(),
)
```

`OppfølgingsoppgaveDeteksjonService.detekter(behandlingId)` returnerer `OppfølgingData?` (null = ikke detektert).
Deduplication-sjekk sammenligner `OppfølgingData` mot forrige rad.

---

## Deduplication

### Sammenligning med eksisterende oppfølgingsliste

Eksisterende oppfølgingsliste kjøres manuelt og gjør:
1. Marker alle aktive oppfølginger som inaktive
2. Re-evaluerer alle behandlinger
3. Hopper kun over innslag der siste utfall var `IGNORERES` **og** data er uendret

### Strategi for oppfølgingsoppgaver: re-opprett ved dataendring

For dataendring-sjekken:
1. Hent siste rad fra DB for `behandlingId`
2. Beregn ny `OppfølgingData` fra deteksjonen
3. **Kun opprett ny GSYS-oppgave** hvis:
   - Ingen tidligere rad finnes, **eller**
   - `data` har endret seg siden sist

Dette betyr at endringer i underliggende data gir ny GSYS-oppgave,
mens en uendret situasjon ikke spammer nye oppgaver.

---

## Deteksjon – case X: overgangsstønad er stanset

Case X detekteres ved å gjenbruke eksisterende logikk fra `OppfølgingOpprettKontrollerService`, men filtrert til kun `OVERGANGSSTØNAD`:

1. Hent vedtaksperioder og inngangsvilkår (oppfylte vilkårperioder) for behandlingen
2. Filtrer inngangsvilkår til kun `MålgruppeType.OVERGANGSSTØNAD`
3. Hent registerytelser for overgangsstønad (`TypeYtelsePeriode.ENSLIG_FORSØRGER`)
4. Kjør `OppfølgingMålgruppeKontrollerUtil.finnEndringer(...)` – returnerer `List<PeriodeForKontroll>`
5. Filtrer til perioder med `TOM_ENDRET` eller `INGEN_TREFF`
6. Hvis noen treff → returner `OppfølgingData(perioderTilKontroll = treff)`, ellers `null`

`OppfølgingsoppgaveDeteksjonService` er **ikke lenger en stub** – den implementeres fullt ut.

GSYS-oppgavebeskrivelsen bør gi saksbehandler kontekst. For hver periode med `TOM_ENDRET` eller `INGEN_TREFF`:

> "Overgangsstønad er registrert til \<dato x\>, men bruker har vedtaksperioder som strekker seg til \<dato y\>. Vurder stans av \<stønadstype\>."

Der `<dato x>` er TOM fra registeret (eller "ingen registrering" ved `INGEN_TREFF`), `<dato y>` er TOM på vedtaksperioden, og `<stønadstype>` er aktuell stønadstype.

---

## Endringer i eksisterende filer

### OppgaveUtil.kt

`utledBehandlesAvApplikasjon()` og `skalHåndteresAvTSSak()` har `else -> error(...)` som vil kaste
exception dersom `VurderKonsekvensForYtelse` brukes. Begge må oppdateres **med samme verdier som `BehandleSak`**:

```kotlin
Oppgavetype.VurderKonsekvensForYtelse -> "tilleggsstonader-sak"  // utledBehandlesAvApplikasjon
Oppgavetype.VurderKonsekvensForYtelse -> true                    // skalHåndteresAvTSSak → KLAR-mappe
```

**Angående bekymringen om "alle andre" VurderKonsekvensForYtelse-oppgaver:**
TS-sak sin oppgaveliste filtrerer på enhet-spesifikk `mappeId` (KLAR/PÅ_VENT). Oppgaver andre
systemer oppretter havner aldri i vår KLAR-mappe → bekymringen er allerede håndtert av mappeId-filteret.

### DatabaseConfiguration.kt

Ingen endring nødvendig – `OppfølgingDataReader`/`OppfølgingDataWriter` eksisterer allerede og håndterer `OppfølgingData`-typen som vi gjenbruker.

### Toggle.kt

Nytt feature flag: `OPPRETT_OPPFØLGINGSOPPGAVE`

```kotlin
OPPRETT_OPPFØLGINGSOPPGAVE("sak.opprett-oppfolgingsoppgave")
```

`DagligOppfølgingsoppgaveTask` sjekker flagget før den oppretter sub-tasks.
Når flagget er av: kjøringen stoppes → ingen nye oppfølgingsoppgaver opprettes.

---

## Todos (i rekkefølge)

1. **oppgaveutil-fix** – Oppdater `OppgaveUtil.kt` med `VurderKonsekvensForYtelse` (`true`/`"tilleggsstonader-sak"`)
2. **toggle** – Legg til `OPPRETT_OPPFØLGINGSOPPGAVE` i `Toggle.kt`
3. **db-migration** – Flyway-migrering V122 for `oppfolgingsoppgave`-tabell
4. **entity** – `Oppfølgingsoppgave.kt` – entity som gjenbruker `OppfølgingData` som datatype (ingen ny enum/sealed class)
5. **repository** – `OppfølgingsoppgaveRepository.kt` – inkl. `finnSisteForBehandling(behandlingId)`
6. **deteksjon** – `OppfølgingsoppgaveDeteksjonService.kt` (implementert: filtrerer til OVERGANGSSTØNAD, gjenbruker `OppfølgingMålgruppeKontrollerUtil`, returnerer `OppfølgingData?`)
7. **service** – `OppfølgingsoppgaveService.kt` (deduplication + opprettelse)
8. **per-behandling-task** – `OppfølgingsoppgaveBehandlingTask.kt`
9. **daglig-task** – `DagligOppfølgingsoppgaveTask.kt` (self-scheduling)

---

## Åpne spørsmål

- **Gjenbruk av eksisterende oppfølgings-deteksjon**: Deteksjonslogikken for case X gjenbruker `OppfølgingMålgruppeKontrollerUtil` + `OppfølgingOpprettKontrollerService`, filtrert til kun `OVERGANGSSTØNAD`.
