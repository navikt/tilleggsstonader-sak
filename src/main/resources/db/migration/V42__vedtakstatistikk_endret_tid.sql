ALTER TABLE vedtaksstatistikk
    ADD COLUMN endret_tid TIMESTAMP(3);

UPDATE vedtaksstatistikk SET endret_tid = opprettet_tid;

ALTER TABLE vedtaksstatistikk ALTER COLUMN endret_tid SET NOT NULL;