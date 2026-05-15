package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class VarselDittNavKafkaProducer(
    val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${kjøreliste-skjema.url}")
    private lateinit var kjørelisteSkjemaUrl: String

    @Value("\${topics.dittnav}")
    private lateinit var topic: String

    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var cluster: String

    @Value("\${NAIS_NAMESPACE}")
    private lateinit var namespace: String

    @Value("\${NAIS_APP_NAME}")
    private lateinit var appName: String

    fun sendVarselOmKjørelisterTilgjengelig(
        fnr: String,
        varselId: String,
    ) {
        val kafkaBeskjedJson = lagKafkaBeskjedJson(fnr = fnr, varselId = varselId)
        val producerRecord = ProducerRecord(topic, varselId, kafkaBeskjedJson)
        kafkaTemplate.send(producerRecord).get()
    }

    private fun lagKafkaBeskjedJson(
        fnr: String,
        varselId: String,
    ) = VarselActionBuilder.opprett {
        type = Varseltype.Beskjed
        this.varselId = varselId
        sensitivitet = Sensitivitet.Substantial
        ident = fnr
        tekster +=
            Tekst(
                spraakkode = "nb",
                tekst = KJØRELISTE_TILGJENGELIG_MELDING,
                default = true,
            )
        link = kjørelisteSkjemaUrl
        eksternVarsling { preferertKanal = EksternKanal.SMS }
        produsent =
            Produsent(
                cluster = cluster,
                namespace = namespace,
                appnavn = appName,
            )
    }

    companion object {
        const val KJØRELISTE_TILGJENGELIG_MELDING = "Du har én eller flere kjørelister tilgjengelige for utfylling."
    }
}
