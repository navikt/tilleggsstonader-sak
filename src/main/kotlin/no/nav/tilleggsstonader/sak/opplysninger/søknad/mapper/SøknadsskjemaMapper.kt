package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn

object SøknadsskjemaMapper {
    fun map(skjema: Søknadsskjema<SøknadsskjemaBarnetilsyn>, journalpostId: String): SøknadBarnetilsyn {
        return SøknadBarnetilsyn(
            journalpostId = journalpostId,
            mottattTidspunkt = skjema.mottattTidspunkt,
            språk = skjema.språk,
            barn = skjema.skjema.barn.barnMedBarnepass.map {
                SøknadBarn(
                    ident = it.ident.verdi,
                )
            }.toSet(),
        )
    }
}
