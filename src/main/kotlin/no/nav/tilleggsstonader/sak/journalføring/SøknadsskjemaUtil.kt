package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler

object SøknadsskjemaUtil {
    fun parseSøknadsskjema(stønadstype: Stønadstype, data: ByteArray): Søknadsskjema<out Skjema> {
        return when (stønadstype) {
            Stønadstype.BARNETILSYN -> objectMapper.readValue<Søknadsskjema<SøknadsskjemaBarnetilsyn>>(data)
            Stønadstype.LÆREMIDLER -> objectMapper.readValue<Søknadsskjema<SøknadsskjemaLæremidler>>(data)
        }
    }
}
