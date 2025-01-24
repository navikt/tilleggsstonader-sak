UPDATE vilkarperioder_grunnlag
SET grunnlag = replace(grunnlag::text, 'ensligForsørgerStønadstype', 'subtype')::json;