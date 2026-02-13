package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapperFailOnUnknownProperties
import no.nav.tilleggsstonader.kontrakter.felles.Språkkode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.Skjemadata
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaBoutgifterFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaDagligReiseFyllUtSendInn
import no.nav.tilleggsstonader.kontrakter.søknad.SøknadsskjemaLæremidler
import no.nav.tilleggsstonader.kontrakter.søknad.dagligreise.fyllutsendinn.OppholdUtenforNorge
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.readValue
import tools.jackson.module.kotlin.treeToValue
import java.time.LocalDateTime

object SøknadsskjemaUtil {
    val jsonMapperMedCustomDeserializerForDagligReise =
        jsonMapperFailOnUnknownProperties
            .rebuild()
            .addModule(SimpleModule().addDeserializer(OppholdUtenforNorge::class.java, OppholdUtenforNorgeDeserializer()))
            .build()

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
        val skjema = jsonMapperMedCustomDeserializerForDagligReise.readValue<SøknadsskjemaDagligReiseFyllUtSendInn>(data)
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

class OppholdUtenforNorgeDeserializer : StdDeserializer<OppholdUtenforNorge>(OppholdUtenforNorge::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?,
    ): OppholdUtenforNorge? {
        val jsonNode = p.readValueAsTree<JsonNode>()
        return if (jsonNode.isEmpty) {
            null
        } else {
            jsonMapper.treeToValue<OppholdUtenforNorge>(jsonNode)
        }
    }
}
