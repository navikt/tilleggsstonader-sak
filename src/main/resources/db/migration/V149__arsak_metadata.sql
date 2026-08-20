ALTER TABLE behandling
    RENAME COLUMN nye_opplysninger_kilde TO arsak_metadata_kilde;

ALTER TABLE behandling
    RENAME COLUMN nye_opplysninger_beskrivelse TO arsak_metadata_beskrivelse;

ALTER TABLE behandling
    RENAME COLUMN nye_opplysninger_endringer TO arsak_metadata_endringer;
