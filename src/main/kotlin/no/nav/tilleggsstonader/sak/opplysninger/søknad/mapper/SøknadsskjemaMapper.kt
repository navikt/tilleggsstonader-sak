package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SøknadsskjemaBarnetilsyn

object SøknadsskjemaMapper {
    fun map(skjema: SøknadsskjemaBarnetilsyn, journalpostId: String): SøknadBarnetilsyn {
        return SøknadBarnetilsyn(
            journalpostId = journalpostId,
            datoMottatt = skjema.datoMottatt,
            barn = skjema.barn.map { SøknadBarn(fødselsnummer = it.fødselsnummer, navn = it.navn) }.toSet(),
        )
    }
}
