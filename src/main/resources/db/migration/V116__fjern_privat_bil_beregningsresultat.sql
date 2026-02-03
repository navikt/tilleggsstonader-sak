UPDATE vedtak
SET data =
        jsonb_set(
                data,
                '{beregningsresultat}',
                (data::jsonb -> 'beregningsresultat') - 'privatBil'
        )::json
WHERE data::jsonb -> 'beregningsresultat' ? 'privatBil';