package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import java.time.LocalDate

object VilkårsoppsummeringUtil {
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
