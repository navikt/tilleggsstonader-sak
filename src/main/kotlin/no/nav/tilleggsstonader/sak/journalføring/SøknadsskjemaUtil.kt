package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.Skjemadata
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadKjøreliste
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime

object SøknadsskjemaUtil {
    fun parseSøknadsskjema(
        stønadstype: Stønadstype,
        data: ByteArray,
        mottattTidspunkt: LocalDateTime,
    ): InnsendtSkjema<out Skjemadata> =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> jsonMapper.readValue<InnsendtSkjema<SøknadsskjemaBarnetilsyn>>(data)
            Stønadstype.LÆREMIDLER -> jsonMapper.readValue<InnsendtSkjema<SøknadsskjemaLæremidler>>(data)
            Stønadstype.BOUTGIFTER -> håndterBoutgifter(data, mottattTidspunkt)
            Stønadstype.DAGLIG_REISE_TSO -> håndterDagligReise(data, mottattTidspunkt)
            Stønadstype.DAGLIG_REISE_TSR -> håndterDagligReise(data, mottattTidspunkt)
        }

    fun parseKjøreliste(data: ByteArray): InnsendtSkjema<KjørelisteSkjema> {
        val skjema = jsonMapperFailOnUnknownProperties.readValue<SøknadKjøreliste>(data)
        return InnsendtSkjema(
            ident = skjema.id.toString(),
            mottattTidspunkt = skjema.mottattTidspunkt,
            språk = skjema.språk,
            skjema =
                KjørelisteSkjema(
                    reisedagerPerUkeAvsnitt = skjema.data.reiser,
                    dokumentasjon = skjema.data.dokumentasjon,
                ),
        )
    }

    private fun håndterBoutgifter(
        data: ByteArray,
        mottattTidspunkt: LocalDateTime,
    ): InnsendtSkjema<SøknadsskjemaBoutgifterFyllUtSendInn> {
        val skjema = jsonMapperFailOnUnknownProperties.readValue<SøknadsskjemaBoutgifterFyllUtSendInn>(data)
        return InnsendtSkjema(
            ident = skjema.data.data.dineOpplysninger.identitet.identitetsnummer,
            mottattTidspunkt = mottattTidspunkt,
            språk = mapFyllUtSpråk(skjema.language),
            skjema = skjema,
        )
    }

    private fun håndterDagligReise(
        data: ByteArray,
        mottattTidspunkt: LocalDateTime,
    ): InnsendtSkjema<SøknadsskjemaDagligReiseFyllUtSendInn> {
        secureLogger.info("Deserialiserer daglig reise skjema: \n ${String(data)}")
        val skjema = jsonMapperFailOnUnknownProperties.readValue<SøknadsskjemaDagligReiseFyllUtSendInn>(data)
        return InnsendtSkjema(
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
