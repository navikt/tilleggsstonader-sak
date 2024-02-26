# tilleggsstonader-sak

Backend - saksbehandling for tilleggsstønader

## Kjøring lokalt

### Client id & client secret

secret kan hentes fra cluster:

1. `gcloud auth login`
2. `brew install jq` hvis du mangler det.
3. `kubectl --context dev-gcp -n tilleggsstonader get secret azuread-tilleggsstonader-sak-lokal -o json | jq '.data | map_values(@base64d)' | grep CLIENT`

Variablene legges inn under SakAppLocal -> Edit Configurations -> Modify Options -> huk av for Environemntal Variables
Legg inn `AZURE_APP_CLIENT_ID={secret},AZURE_APP_CLIENT_SECRET={secret}`

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
