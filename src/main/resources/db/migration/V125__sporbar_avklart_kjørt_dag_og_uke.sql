ALTER TABLE avklart_kjort_dag
    ADD COLUMN opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3),
    ADD COLUMN endret_av     VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN endret_tid    TIMESTAMP    NOT NULL DEFAULT current_timestamp;

ALTER TABLE avklart_kjort_dag
    ALTER COLUMN opprettet_av DROP DEFAULT,
    ALTER COLUMN opprettet_tid DROP DEFAULT,
    ALTER COLUMN endret_av DROP DEFAULT,
    ALTER COLUMN endret_tid DROP DEFAULT;

ALTER TABLE avklart_kjort_uke
    ADD COLUMN opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3),
    ADD COLUMN endret_av     VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN endret_tid    TIMESTAMP    NOT NULL DEFAULT current_timestamp;

ALTER TABLE avklart_kjort_uke
    ALTER COLUMN opprettet_av DROP DEFAULT,
    ALTER COLUMN opprettet_tid DROP DEFAULT,
    ALTER COLUMN endret_av DROP DEFAULT,
    ALTER COLUMN endret_tid DROP DEFAULT;
