package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
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

    fun harBarnUnder2ÅrIStønadsperiode(
        barn: List<GrunnlagBarn>,
        stønadsperioder: List<StønadsperiodeDto>,
    ) = barn.any { barn ->
        val fødselsdato = barn.fødselsdato
        fødselsdato == null || erUnder2ÅrIEnStønadsperiode(stønadsperioder, fødselsdato)
    }

    private fun erUnder2ÅrIEnStønadsperiode(
        stønadsperioder: List<StønadsperiodeDto>,
        fødselsdato: LocalDate,
    ) = stønadsperioder.any { stønadsperiode ->
        stønadsperiode.fom <= fødselsdato.plusYears(2) &&
            stønadsperiode.tom >= fødselsdato
    }
}
