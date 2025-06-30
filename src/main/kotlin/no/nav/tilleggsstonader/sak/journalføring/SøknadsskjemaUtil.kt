package no.nav.tilleggsstonader.sak.journalføring

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.Skjema
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import java.time.LocalDateTime

object SøknadsskjemaUtil {
    fun parseSøknadsskjema(
        stønadstype: Stønadstype,
        data: ByteArray,
        mottattTidspunkt: LocalDateTime,
    ): Søknadsskjema<out Skjema> =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> objectMapper.readValue<Søknadsskjema<SøknadsskjemaBarnetilsyn>>(data)
            Stønadstype.LÆREMIDLER -> objectMapper.readValue<Søknadsskjema<SøknadsskjemaLæremidler>>(data)
            Stønadstype.BOUTGIFTER -> håndterBoutgifter(data, mottattTidspunkt)
            Stønadstype.DAGLIG_REISE_TSO -> TODO()
            Stønadstype.DAGLIG_REISE_TSR -> TODO()
        }

    private fun håndterBoutgifter(
        data: ByteArray,
        mottattTidspunkt: LocalDateTime,
    ): Søknadsskjema<SøknadsskjemaBoutgifterFyllUtSendInn> {
        val skjema = objectMapperFailOnUnknownProperties.readValue<SøknadsskjemaBoutgifterFyllUtSendInn>(data)
        return Søknadsskjema(
            ident = skjema.data.data.dineOpplysninger.identitet.identitetsnummer,
            mottattTidspunkt = mottattTidspunkt,
            språk = mapFyllUtSpråk(skjema.language),
            skjema = skjema,
        )
    }

    private fun mapFyllUtSpråk(språk: String): Språkkode =
        when (språk) {
            "nb-NO" -> Språkkode.NB
            "nn-NO" -> Språkkode.NN
            else -> error("Har ikke mapping for språk=$språk")
        }
}
