---
applyTo: "**/*IntegrationTest.kt"
---

# Integration Test Guidelines

Integration tests (`*IntegrationTest.kt`) verify high-level end-to-end behaviour across the full behandlingsflyt. They extend `IntegrationTest` and use the helper functions in `GjennomførBehandling.kt` together with the `BehandlingTestdataDsl` to drive a behandling through its steps.

## Class Structure

```kotlin
class MinFunksjonalitetIntegrationTest : IntegrationTest() {

    @Test
    fun `beskriver hva som testes`() {
        // ...
    }
}
```

## Driving a behandling from søknad to ferdigstilt

Use `opprettBehandlingOgGjennomførBehandlingsløp` as the primary entry point. It creates a journalpost, håndterer søknaden, runs all steg, and returns the `BehandlingId`.

```kotlin
val behandlingId = opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.BARNETILSYN) {
    defaultTilsynBarnTestdata()
}
```

Available `default*Testdata()` shortcuts in `BehandlingTestdataDsl`:

| Metode                                                                            | Stønadstype                       |
|-----------------------------------------------------------------------------------|-----------------------------------|
| `defaultTilsynBarnTestdata(fom, tom)`                                             | `BARNETILSYN`                     |
| `defaultLæremidlerTestdata(fom, tom)`                                             | `LÆREMIDLER`                      |
| `defaultBoutgifterTestdata(fom, tom)`                                             | `BOUTGIFTER`                      |
| `defaultDagligReiseTsoTestdata(fom, tom)`                                         | `DAGLIG_REISE_TSO`                |
| `defaultDagligReiseTsrTestdata(fom, tom)`                                         | `DAGLIG_REISE_TSR`                |
| `defaultDagligReisePrivatBilTsoTestdata(fom, tom, reiseavstandEnVei, delperioder, hentAktivitetId)` | `DAGLIG_REISE_TSO` med privat bil |

## Customising testdata with the DSL

When the defaults don't cover the scenario, compose the DSL blocks directly:

```kotlin
opprettBehandlingOgGjennomførBehandlingsløp(Stønadstype.BARNETILSYN) {
    val fom = 1 januar 2026
    val tom = 28 februar 2026

    aktivitet {
        opprett {
            aktivitetTiltakTilsynBarn(fom, tom, aktivitetsdager = 3)
        }
    }
    målgruppe {
        opprett {
            målgruppeAAP(fom, tom)
        }
    }
    vilkår {
        opprett {
            passBarn(fom = fom.toYearMonth(), tom = tom.toYearMonth(), utgift = 2000)
        }
    }
    // vedtak defaults to OpprettInnvilgelse — override when needed:
    vedtak {
        opphør(
            årsaker = listOf(ÅrsakOpphør.ANNET),
            begrunnelse = "søker har flyttet",
            opphørsdato = fom,
        )
    }
}
```

**Aktivitet builders:** `aktivitetTiltakTilsynBarn`, `aktivitetTiltakBoutgifter`, `aktivitetTiltakTso`, `aktivitetTiltakTsr`, `aktivitetUtdanningLæremidler`, `aktivitetUtdanningDagligReiseTso`

**Målgruppe builders:** `målgruppeAAP`, `målgruppeTiltakspenger`, `målgruppeDagpenger`, `målgruppeOvergangsstønad`

**Vilkår builders:** `offentligTransport`, `privatBil`, `løpendeutgifterEnBolig`, `passBarn`

**Vedtak builders:** `innvilgelse(vedtaksperioder?)`, `opphør(årsaker, begrunnelse, opphørsdato)`, `avslag()`

To update or delete an existing period (e.g. in a revurdering), use either the single-period helpers or the generic lambda forms:

```kotlin
aktivitet {
    // Single-period helpers (recommended when there is exactly one period):
    oppdaterTomPåEnesteAktivitet(28 februar 2026)
    oppdaterEnesteAktivitet { copy(begrunnelse = "oppdatert") }

    // Generic form:
    oppdater { perioder, behandlingId ->
        val eksisterende = perioder.first()
        eksisterende.id to lagreVilkårperiodeAktivitet(behandlingId, ...)
    }
    slett { perioder ->
        perioder.first().id to SlettVikårperiode(kommentar = "utgått")
    }
}

målgruppe {
    oppdaterTomPåEnesteMålgruppe(28 februar 2026)
    oppdaterEnesteMålgruppe { copy(begrunnelse = "oppdatert") }
}
```

## Stopping at an intermediate steg

Use `tilSteg` to test the state at a specific point in the flow:

```kotlin
val behandlingId = opprettBehandlingOgGjennomførBehandlingsløp(
    stønadstype = Stønadstype.LÆREMIDLER,
    tilSteg = StegType.SIMULERING,
) {
    defaultLæremidlerTestdata()
}

// Assert on the state at SIMULERING
val vedtak = kall.vedtak.hent(behandlingId)
assertThat(vedtak.vedtaksperioder).hasSize(1)
```

Supported `tilSteg` stop-points in the integration helpers: `INNGANGSVILKÅR`, `VILKÅR`, `KJØRELISTE`, `BEREGNING` / `BEREGNE_YTELSE`, `SIMULERING`, `SEND_TIL_BESLUTTER`, `BESLUTTE_VEDTAK`, `BEHANDLING_FERDIGSTILT` (default).

## Testing error cases / expected HTTP status

Use `gjennomførBeregningStegKall` to get the raw `ResponseSpec` and assert on the status code:

```kotlin
gjennomførBeregningStegKall(behandlingId, Stønadstype.BARNETILSYN, OpprettInnvilgelse())
    .expectStatus()
    .isBadRequest
```

## Revurdering

```kotlin
// From an existing behandlingId (reuses same fagsak):
val revurderingId = opprettRevurderingOgGjennomførBehandlingsløp(
    fraBehandlingId = behandlingId,
) {
    defaultTilsynBarnTestdata()
}

// From a custom OpprettBehandlingDto:
val revurderingId = opprettRevurderingOgGjennomførBehandlingsløp(
    opprettBehandlingDto = OpprettBehandlingDto(
        fagsakId = fagsak.id,
        årsak = BehandlingÅrsak.SØKNAD,
        kravMottatt = LocalDate.now(),
        nyeOpplysningerMetadata = null,
    ),
) {
    defaultTilsynBarnTestdata()
}
```

When you only need the `BehandlingId` of a revurdering (before running the full flow), use `opprettRevurdering(opprettBehandlingDto)`.

## Henleggelse

```kotlin
val behandlingId = gjennomførHenleggelse()
// or provide a custom journalpost:
val behandlingId = gjennomførHenleggelse(fraJournalpost = minJournalpost)
```

## Asserting on results

After the flow, use `kall.*` to read state and assert:

```kotlin
val behandling = kall.behandling.hent(behandlingId)
assertThat(behandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)

val vedtak = kall.vedtak.hent(behandlingId)
assertThat(vedtak.vedtaksperioder).hasSize(2)

val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandlingId)
assertThat(vilkårperioder.vilkårperioder.aktiviteter).hasSize(1)
```

## Do and Don't

- **Do** use `opprettBehandlingOgGjennomførBehandlingsløp` instead of manually wiring each steg.
- **Do** use `default*Testdata()` for the happy path, then override only what the test needs.
- **Do** stop at a specific `StegType` when the test is about state before ferdigstilling.
- **Do** use `gjennomførBeregningStegKall` (returns `ResponseSpec`) when asserting non-2xx responses.
- **Don't** call individual `gjennomførXxxSteg` helpers directly unless the test is specifically about a single step in isolation.
- **Don't** duplicate setup code that `BehandlingTestdataDsl` already provides.
- **Don't** use raw English identifiers for domain concepts — keep `behandling`, `fagsak`, `vedtak`, `vilkår`, `stønadstype` in Norwegian as in the codebase.
