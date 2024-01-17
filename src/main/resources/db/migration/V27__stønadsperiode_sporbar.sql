ALTER TABLE stonadsperiode
    ADD COLUMN opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3),
    ADD COLUMN endret_av     VARCHAR      NOT NULL DEFAULT 'VL',
    ADD COLUMN endret_tid    TIMESTAMP(3) NOT NULL DEFAULT current_timestamp(3);

ALTER TABLE stonadsperiode
    ALTER COLUMN opprettet_av DROP DEFAULT,
    ALTER COLUMN opprettet_tid DROP DEFAULT,
    ALTER COLUMN endret_av DROP DEFAULT,
    ALTER COLUMN endret_tid DROP DEFAULT;
