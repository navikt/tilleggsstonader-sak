-- Fiks tidligste_endring for daglig reise innvilgelse-vedtak.
-- Commit 3f2096cd4 (20. mai 2026) forskjøv beregningsplanens fraDato med -29 dager for daglig reise,
-- men legacyTidligsteEndring() returnerte fortsatt fraDato, slik at vedtak.tidligste_endring
-- ble satt 29 dager for tidlig.
--
-- Korrigerer kun vedtak opprettet etter den problematiske commiten (deployet 20. mai 2026).
-- Eldre vedtak fikk fraDato backfyllt fra tidligste_endring i V123 uten offset, og er korrekte.

UPDATE vedtak
SET tidligste_endring = tidligste_endring + INTERVAL '29 days',
    data = jsonb_set(
        data,
        '{beregningsplan,tidligsteEndring}',
        to_jsonb(to_char(tidligste_endring + INTERVAL '29 days', 'YYYY-MM-DD'))
    )
WHERE data ->> 'type' = 'INNVILGELSE_DAGLIG_REISE'
  AND tidligste_endring IS NOT NULL
  AND data -> 'beregningsplan' ->> 'omfang' = 'FRA_DATO'
  AND data -> 'beregningsplan' ->> 'fraDato' = to_char(tidligste_endring, 'YYYY-MM-DD')
  AND opprettet_tid >= '2026-05-20 09:44:20';
