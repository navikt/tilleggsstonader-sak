INSERT INTO fakta_grunnlag (id, behandling_id, data, type, type_id, opprettet_av, opprettet_tid, endret_av, endret_tid)
    (select gen_random_uuid(),
            behandling_id,
            ('{
              "type": "PERSONOPPLYSNINGER"
            }'::jsonb || (grunnlag::jsonb - 'arena')),
            'PERSONOPPLYSNINGER',
            null,
            opprettet_av,
            opprettet_tid,
            endret_av,
            endret_tid
     from grunnlagsdata);

INSERT INTO fakta_grunnlag (id, behandling_id, data, type, type_id, opprettet_av, opprettet_tid, endret_av, endret_tid)
    (select gen_random_uuid(),
            behandling_id,
            ('{
              "type": "ARENA_VEDTAK_TOM"
            }'::jsonb || coalesce(grunnlag::jsonb -> 'arena', '{"vedtakTom": null}'::jsonb)),
            'ARENA_VEDTAK_TOM',
            null,
            opprettet_av,
            opprettet_tid,
            endret_av,
            endret_tid
     from grunnlagsdata);
