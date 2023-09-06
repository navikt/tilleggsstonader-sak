# tilleggsstonader-sak

Backend - saksbehandling for tilleggsstønader

## Kjøring lokalt

### Client id & client secret
secret kan hentes fra cluster med
`kubectl -n tilleggsstonader get secret azuread-tilleggsstonader-sak-lokal -o json | jq '.data | map_values(@base64d)' | grep CLIENT`

* `AZURE_APP_CLIENT_ID` (fra secret)
* `AZURE_APP_CLIENT_SECRET` (fra secret)

Variablene legges inn under ApplicationLocal -> Edit Configurations -> Environment Variables.

### Kjøring med temp-database
For å kjøre opp appen lokalt, kan en kjøre `AppLocal`.

### Kjøring med postgres-database
For å kjøre opp appen lokalt med en postgres-database, kan en kjøre `AppLocalPostgres`.
App'en vil starte opp en container med siste versjon av postgres.

For å kjøre opp postgres containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volymen `docker-compose down -v`


#### Vurderinger
Burde vi sette konfigurere en egen objectMapper, eller bruke eks `Jackson2ObjectMapperBuilder.json().build()` 
som brukes som default i `MappingJackson2HttpMessageConverter`

#### Kommentarer
 * Har ikke tatt med flagget migrert på fagsak då det er uklart om vi skal migrere
#### Saker som mangler
* TilgangService
* AuditLogger - Denne har avhengighet til Tilgang som er response fra integrasjoner