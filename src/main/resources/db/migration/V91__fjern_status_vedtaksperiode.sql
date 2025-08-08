update vedtak
set data = jsonb_set(
        data,
        '{vedtaksperioder}',
        (select jsonb_agg(obj - 'status') from jsonb_array_elements(data -> 'vedtaksperioder') as obj)
           )
where jsonb_path_exists(data, '$.vedtaksperioder[*].status');
