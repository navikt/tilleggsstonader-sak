package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GrunnlagBarn
import java.time.LocalDate

object VilkårsoppsummeringUtil {
    fun harBarnUnder2ÅrIAktivitetsperiode(
        barn: List<GrunnlagBarn>,
        aktivitetsperioder: List<Datoperiode>,
    ) = barn.any { barn ->
        val fødselsdato = barn.fødselsdato
        fødselsdato == null || erUnder2ÅrIEnAktivitetsperiode(aktivitetsperioder, fødselsdato)
    }

    private fun erUnder2ÅrIEnAktivitetsperiode(
        aktivitetsperioder: List<Datoperiode>,
        fødselsdato: LocalDate,
    ) = aktivitetsperioder.any { aktivitetsperiode ->
        aktivitetsperiode.fom <= fødselsdato.plusYears(2) &&
            aktivitetsperiode.tom >= fødselsdato
    }
}
