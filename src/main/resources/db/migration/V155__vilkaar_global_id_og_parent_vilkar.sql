-- Nullable inntil eksisterende vilkår er migrert. Nye vilkår får global_id når de opprettes.
ALTER TABLE vilkar ADD COLUMN global_id UUID NULL;
CREATE UNIQUE INDEX vilkar_behandling_global_id_idx ON vilkar (behandling_id, global_id);

-- Peker på global_id til paraplyvilkåret i samme behandling. Ingen foreign key, siden global_id ikke er unik på tvers av behandlinger.
ALTER TABLE vilkar ADD COLUMN parent_vilkar UUID NULL;
