package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object VilkårsoppsummeringUtil {

    fun utledAlderNårStønadsperiodeBegynner(fødselsdato: LocalDate?, datoFørsteStønadsperidoe: LocalDate?): Int? {
        if (datoFørsteStønadsperidoe == null) {
            return null
        }
        if (fødselsdato == null) {
            return 0
        }
        return ChronoUnit.YEARS.between(fødselsdato, datoFørsteStønadsperidoe).toInt()
    }
}
