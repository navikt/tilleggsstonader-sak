# Copilot Instructions – tilleggsstonader-sak

Backend saksbehandlingssystem for tilleggsstønader (barnetilsyn, læremidler, boutgifter, daglig reise). Bygget med Kotlin + Spring Boot 4 på Java 21, PostgreSQL og Kafka. Deployes til NAIS/GCP.

## Bygg, test og lint

```bash
# Bygg
./gradlew build

# Kjør alle tester
./gradlew test

# Kjør én enkelt testklasse
./gradlew test --tests "no.nav.tilleggsstonader.sak.behandling.BehandlingServiceTest"

# Kjør én enkelt testmetode
./gradlew test --tests "no.nav.tilleggsstonader.sak.behandling.BehandlingServiceTest.skal gjøre noe"

# Lint (ktlint via Spotless)
./gradlew spotlessKotlinCheck

# Auto-fiks lintfeil
./gradlew spotlessKotlinApply

# Bygg uten lint
./gradlew build -PskipLint
```

## Arkitektur

### Domeneoversikt

Kodebasen er delt inn etter forretningsmessige bounded contexts under `no.nav.tilleggsstonader.sak`:

| Pakke | Ansvar |
|-------|--------|
| `fagsak/` | Aggregatrot – én fagsak per person per stønadstype |
| `behandling/` | Saksbehandlingsinstans knyttet til en fagsak |
| `behandlingsflyt/` | Stegbasert arbeidsflyt (se `BehandlingSteg<T>`) |
| `vilkår/` | Vilkårsvurdering og vilkårperioder |
| `vedtak/` | Vedtaksfatting og beregning per stønadstype |
| `utbetaling/` | Tilkjent ytelse, simulering og iverksetting |
| `brev/` | Generering og distribusjon av vedtaksbrev |
| `opplysninger/` | Integrasjoner mot eksterne systemer (PDL, Arena, oppgave osv.) |
| `infrastruktur/` | Tverrgående konfigurasjon, sikkerhet, exception-håndtering |
| `hendelser/` | Kafka-lyttere for journal- og personhendelser |

### Hierarki: FagsakPerson → Fagsak → Behandling → Vedtak

En `FagsakPerson` kan ha én `Fagsak` per `Stønadstype`. En fagsak kan ha flere behandlinger (førstegangsbehandling, revurdering). Et vedtak er knyttet til én behandling.

### Behandlingsflyt (steg)

Behandlinger gjennomgår steg definert i `StegType`-enumen. Hvert steg implementerer `BehandlingSteg<T>` med metodene `utførSteg(saksbehandling, data)` og `nesteSteg(...)`. `StegService` orkestrerer flyten og validerer tilstand.

## Nøkkelkonvensjoner

### Repository-mønster

Kodebasen bruker Spring Data JDBC (ikke JPA). Alle repositories arver `RepositoryInterface<T, ID>` som **kaster feil ved kall til `save()`/`saveAll()`**. Bruk alltid eksplisitt:

```kotlin
repository.insert(entity)
repository.update(entity)
repository.insertAll(liste)
repository.updateAll(liste)
```

### ID-typer som value classes

Alle domene-ID-er er `@JvmInline value class`, f.eks. `BehandlingId`, `FagsakId`, `FagsakPersonId`, `VilkårId`. Bruk disse konsekvent – aldri rå `UUID`.

```kotlin
val id = BehandlingId.random()
val id = BehandlingId.fromString(uuidString)
```

### Dato-notasjon

I ny Kotlin-kode og nye tester skal datoer alltid skrives med infix-funksjonene fra `no.nav.tilleggsstonader.libs.utils.dato`, med fulle månedsnavn:

```kotlin
import no.nav.tilleggsstonader.libs.utils.dato.januar

val fom = 1 januar 2026
```

Bruk ikke `LocalDate.of(2026, 1, 1)` når infix-notasjonen kan brukes.

### Feilhåndtering

Bruk hjelpefunksjonene fra `infrastruktur/exception/Feil.kt`:

```kotlin
brukerfeil("Melding til bruker")                      // ApiFeil – 400-feil, logges som INFO
brukerfeilHvis(condition) { "Melding" }
brukerfeilHvisIkke(condition) { "Melding" }

feil("Systemfeil")                                     // Feil – 500-feil, logges som ERROR
feilHvis(condition) { "Systemfeil" }
feilHvisIkke(condition) { "Systemfeil" }
```

Unngå å kaste `RuntimeException` direkte.

### Feature toggles

Alle feature flags defineres som enum-verdier i `infrastruktur/unleash/Toggle.kt` og sjekkes via `UnleashService`:

```kotlin
if (unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL)) { ... }
```

### REST-endepunkter

- Alle endepunkter ligger under `/api/`
- Forvaltningsendepunkter (intern admin) bruker `/api/forvaltning/`
- Alle controllers annoteres med `@ProtectedWithClaims(issuer = "azuread")`

### Ekstern-klienter

Klienter i `opplysninger/` bruker `RestTemplate` med qualifier `azureClientCredential`. De konfigureres i `infrastruktur/config/ApplicationConfig.kt` med URL-er fra `application.yml` under `clients.*`.

## Testing

### Integrasjonstester

Arv `IntegrationTest` (eller `CleanDatabaseIntegrationTest` for automatisk db-rens mellom tester):

```kotlin
class MinTest : IntegrationTest() { ... }
// eller
class MinTest : CleanDatabaseIntegrationTest() { ... }
```

`IntegrationTest` setter opp Spring Boot-kontekst med MockOAuth2Server og alle nødvendige mock-profiler (`mock-pdl`, `mock-oppgave`, `mock-kafka`, osv.).

### Nyttige testverktøy

| Klasse | Bruk |
|--------|------|
| `TestoppsettService` | Opprett fagsak, behandling og testdata |
| `TokenUtil` | Generer JWT-tokens for ulike autentiseringsflyter |
| `Kall` | Wrapper for HTTP-kall i integrasjonstester |
| `MockClients` | Tilgang til alle mock-klienter for ekstern-stubbings |

### Mocking

- Bruk **MockK** (ikke Mockito): `every { service.method() } returns result`
- HTTP-stubbings: **WireMock**
- Ekstern-klienter: mockede via Spring-profiler, tilgjengelig via `mockClients`-bean

### Token-generering i tester

```kotlin
// Saksbehandler/beslutter (standard)
onBehalfOfToken(roller = listOf(rolleConfig.beslutterRolle), saksbehandler = "julenissen")

// System-til-system
clientCredential(clientId = "...", accessAsApplication = true)

// Innbygger (TokenX)
tokenX(clientId = "...", ident = "12345678901")
```

## Lokal kjøring

Start `SakAppLocal` (H2-database i minnet) eller `SakAppLocalPostgres` (Docker PostgreSQL via `docker-compose up`). Secrets hentes fra dev-gcp-clusteret – se README.
