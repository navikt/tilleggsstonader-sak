ALTER TABLE behandling_barn
    DROP COLUMN navn;

ALTER TABLE soknad_barn
    DROP COLUMN navn;

ALTER TABLE behandling_barn RENAME COLUMN person_ident TO ident;
ALTER TABLE soknad_barn RENAME COLUMN fodselsnummer TO ident;

ALTER TABLE soknad RENAME COLUMN dato_mottatt TO mottatt_tidspunkt;
ALTER TABLE soknad ADD COLUMN sprak VARCHAR NOT NULL;