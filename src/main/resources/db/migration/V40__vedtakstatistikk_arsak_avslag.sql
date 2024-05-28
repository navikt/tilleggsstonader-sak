ALTER TABLE vedtaksstatistikk
    ADD COLUMN arsaker_avslag JSON,
    ADD COLUMN opprettet_tid  TIMESTAMP(3);

UPDATE vedtaksstatistikk
SET opprettet_tid = tidspunkt_vedtak;

ALTER TABLE vedtaksstatistikk
    ALTER COLUMN opprettet_tid SET NOT NULL;