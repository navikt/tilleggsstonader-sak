UPDATE vilkar
SET delvilkar = replace(delvilkar::TEXT, 'HAR_ALDER_LAVERE_ENN_GRENSEVERDI', 'HAR_FULLFØRT_FJERDEKLASSE')::jsonb;
