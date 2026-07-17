-- Sletter alle reise-til-samling-søknader fra soknad-tabellen.
-- Bakgrunn: soknad.data lagres som JSON-blob (schema-on-read). Reise-til-samling-skjemaet har endret
-- struktur flere ganger uten datamigrering, så eldre rader kan ikke lenger deserialiseres til dagens
-- SkjemaReiseTilSamling (bl.a. mangler de non-null-feltet erObligatorisk på samlinger).
-- I stedet for å migrere de gamle blobene forkastes de – reise til samling er fortsatt under utvikling
-- og disse søknadene skal ikke tas vare på.
--
-- Reise-til-samling-rader identifiseres ved nøkkelen 'samlinger', som er unik for dette skjemaet.
-- soknad_behandling og soknad_barn har fremmednøkler mot soknad(id) og må ryddes først.
DELETE
FROM soknad_behandling
WHERE soknad_id IN (SELECT id FROM soknad WHERE data::jsonb ? 'samlinger');

-- Antakelig ikke nødvendig, men for å være trygg
DELETE
FROM soknad_barn
WHERE soknad_id IN (SELECT id FROM soknad WHERE data::jsonb ? 'samlinger');

DELETE
FROM soknad
WHERE data::jsonb ? 'samlinger';
