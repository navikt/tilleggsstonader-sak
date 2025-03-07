ALTER TABLE vilkar_periode
    ADD COLUMN git_versjon TEXT;
ALTER TABLE vilkar
    ADD COLUMN git_versjon TEXT;
ALTER TABLE vedtak
    ADD COLUMN git_versjon TEXT;
ALTER TABLE behandlingshistorikk
    ADD COLUMN git_versjon TEXT;
