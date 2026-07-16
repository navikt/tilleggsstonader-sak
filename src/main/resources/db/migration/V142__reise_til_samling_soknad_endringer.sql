-- Migrerer gamle reise-til-samling-søknader i soknad.data til ny struktur.
-- Gamle rader identifiseres ved at de har nøkkelen 'reiseavstand' (unik for gammelt reise-til-samling-skjema).
-- Migreringen er idempotent: etter kjøring finnes ikke 'reiseavstand' lenger, så en ny kjøring treffer ingen rader.
--
-- Endringer:
--   * reiseavstand -> avreiseadresse. skalReiseFraFolkeregistrertAdresse settes til JA (mangler i gamle data).
--   * Feltene fra reiseavstand (antallKilometerEnVei, land, gateadresse, postnummer, poststed) kopieres inn i HVER samling.
--   * Nye samling-felt: erObligatorisk = JA, harBruktEkstraReiseDager = NEI (gjettede verdier).
--   * reisemåte: kanReiseKollektivt -> kanReiseMedOffentligTransport (NEI hvis mangler),
--     totalutgifterKollektivt -> totalUtgifterOffentligTransport, kanBenytteDrosje -> ønskerDekketUtgifterForDrosje,
--     kanBenytteEgenBil beholdes (JaNei-verdiene JA/NEI er gyldige for det nye enum-et).
--   * Nye nullable felt uten kildedata utelates.
UPDATE soknad AS s
SET data =
        (
            (x.d - 'reiseavstand')
                || jsonb_build_object(
                        'avreiseadresse',
                        jsonb_build_object('skalReiseFraFolkeregistrertAdresse', 'JA')
                   )
                || jsonb_build_object(
                        'samlinger',
                        (SELECT COALESCE(
                                        jsonb_agg(
                                                samling || jsonb_build_object(
                                                        'erObligatorisk', 'JA',
                                                        'harBruktEkstraReiseDager', 'NEI',
                                                        'antallKilometerEnVei',
                                                        COALESCE(x.ra ->> 'antallKilometerEnVei', ''),
                                                        'adresse', jsonb_strip_nulls(
                                                                jsonb_build_object(
                                                                        'adresse', x.ra -> 'gateadresse',
                                                                        'postnummer', x.ra -> 'postnummer',
                                                                        'poststed', x.ra -> 'poststed',
                                                                        'landkode', x.ra -> 'land'
                                                                )
                                                        )
                                                )
                                        ),
                                        '[]'::jsonb
                                )
                         FROM jsonb_array_elements(COALESCE(x.d -> 'samlinger', '[]'::jsonb)) AS samling)
                   )
                || jsonb_build_object(
                        'reisemåte',
                        jsonb_strip_nulls(
                                jsonb_build_object(
                                        'kanReiseMedOffentligTransport',
                                        COALESCE(x.d -> 'reisemåte' ->> 'kanReiseKollektivt', 'NEI'),
                                        'totalUtgifterOffentligTransport',
                                        x.d -> 'reisemåte' -> 'totalutgifterKollektivt',
                                        'kanBenytteEgenBil', x.d -> 'reisemåte' -> 'kanBenytteEgenBil',
                                        'ønskerDekketUtgifterForDrosje', x.d -> 'reisemåte' -> 'kanBenytteDrosje'
                                )
                        )
                   )
        )::json
FROM (SELECT id,
             data::jsonb                   AS d,
             data::jsonb -> 'reiseavstand' AS ra
      FROM soknad
      WHERE data::jsonb ? 'reiseavstand') AS x
WHERE s.id = x.id;
