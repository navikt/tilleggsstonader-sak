package no.nav.tilleggsstonader.sak.opplysninger.søknad.skjema

import java.time.LocalDateTime

data class SøknadsskjemaBarnetilsyn(
    val datoMottatt: LocalDateTime,
    val barn: List<SkjemaBarn>,
)

data class SkjemaBarn(
    val fødselsnummer: String,
    val navn: String,
)
