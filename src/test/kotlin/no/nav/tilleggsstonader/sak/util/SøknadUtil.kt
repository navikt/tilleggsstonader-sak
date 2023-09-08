package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SkjemaBarn
import no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema.SøknadsskjemaBarnetilsyn
import java.time.LocalDateTime

object SøknadUtil {

    fun søknadskjemaBarnetilsyn(
        datoMottatt: LocalDateTime = LocalDateTime.now(),
        barn: List<SkjemaBarn> = listOf(skjemaBarn()),
    ) = SøknadsskjemaBarnetilsyn(
        datoMottatt = datoMottatt,
        barn = barn,
    )

    fun skjemaBarn(
        fødselsnummer: String = "fnr",
        navn: String = "navn",
    ) = SkjemaBarn(
        fødselsnummer = fødselsnummer,
        navn = navn,
    )
}
