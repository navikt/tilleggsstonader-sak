DROP INDEX oppfolging_behandling_id_idx;

CREATE UNIQUE INDEX oppfolging_behandling_id_aktiv_idx ON oppfolging (behandling_id) WHERE aktiv = true;
CREATE INDEX oppfolging_behandling_id_idx ON oppfolging (behandling_id);