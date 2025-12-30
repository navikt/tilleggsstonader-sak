# tilleggsstonader-sak

Backend - saksbehandling for tilleggsstønader

## Kjøring lokalt

### Secrets

Nødvendige secrets kan hentes fra cluster:

1. `gcloud auth login`
2. `brew install jq` hvis du mangler det.
3. `kubectl --context dev-gcp -n tilleggsstonader get secret azuread-tilleggsstonader-sak-lokal -o json | jq '.data | map_values(@base64d)' | grep CLIENT`
4. `kubectl --context dev-gcp -n tilleggsstonader get secret google-maps-api-key -o json | jq '.data | map_values(@base64d)'`
5. Legg til en .env-fil i prosjektet med innholdet `AZURE_APP_CLIENT_ID={secret}` , `AZURE_APP_CLIENT_SECRET={secret},` og
  `GOOGLE_MAPS_API_KEY={secret}` og `GOOGLE_MAPS_EMBEDED_MAP_API_KEY={secret}`
6. Variablene legges inn under `SakAppLocal` eller `SakAppLocalPostgres` -> Edit Configurations -> Modify Options -> huk av for Environemntal Variables, og velg `.env`-fila du opprettet 

### Kjøring med temp-database

Kjør opp Spring-appen `SakAppLocal`. Dette starter appen med en midlertidig database som kjører i minnet, og blir tømt
når appen stoppes.
For å kjøre med mer persistente data, se neste punkt.

### Kjøring med postgres-database

For å kjøre opp postgres-containern så kjører man `docker-compose up`
For å ta ned containern så kjører man `docker-compose down`
For å slette volymen `docker-compose down -v`

For lokal kjøring av appen mot postgres-databasen, start opp `SakAppLocalPostgres`. Husk å legge inn
miljøvariablene også her.

## Kode generert av GitHub Copilot

Dette repoet bruker GitHub Copilot til å generere kode.
