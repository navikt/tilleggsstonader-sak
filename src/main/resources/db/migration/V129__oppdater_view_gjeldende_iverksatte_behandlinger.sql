DROP VIEW IF EXISTS gjeldende_iverksatte_behandlinger;

CREATE OR REPLACE VIEW gjeldende_iverksatte_behandlinger AS
SELECT *
FROM (
         SELECT f.stonadstype,
                ROW_NUMBER() OVER (PARTITION BY b.fagsak_id ORDER BY b.vedtakstidspunkt DESC) rn,
                f.fagsak_person_id,
                b.id,
                b.fagsak_id,
                b.versjon,
                b.opprettet_av,
                b.opprettet_tid,
                b.endret_av,
                b.endret_tid,
                b.type,
                b.status,
                b.steg,
                b.kategori,
                b.resultat,
                b.forrige_iverksatte_behandling_id,
                b.arsak,
                b.krav_mottatt,
                b.henlagt_arsak,
                b.vedtakstidspunkt,
                b.henlagt_begrunnelse,
                b.nye_opplysninger_kilde,
                b.nye_opplysninger_endringer,
                b.nye_opplysninger_beskrivelse,
                b.behandling_metode
         FROM behandling b
                  JOIN fagsak f ON b.fagsak_id = f.id
         WHERE b.resultat IN ('OPPHØRT', 'INNVILGET')
           AND b.status = 'FERDIGSTILT'
     ) q
WHERE rn = 1;

