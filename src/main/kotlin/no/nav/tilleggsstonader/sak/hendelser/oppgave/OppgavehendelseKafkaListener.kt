package no.nav.tilleggsstonader.sak.hendelser.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppdatertOppgaveHendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Profile("!local & !integrasjonstest & !prod")
class OppgavehendelseKafkaListener(
    private val oppgavehendelseHåndterer: OppgavehendelseHåndterer,
) {
    @KafkaListener(
        groupId = "tilleggsstonader-sak",
        topics = ["\${topics.oppgavehendelser}"],
        containerFactory = "oppgavehendelserListenerContainerFactory",
    )
    @Transactional
    fun listen(
        consumerRecords: List<ConsumerRecord<String, OppgaveRecord>>,
        acknowledgment: Acknowledgment,
    ) {
        oppgavehendelseHåndterer.behandleOppgavehendelser(consumerRecords)
        acknowledgment.acknowledge()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveRecord(
    val hendelse: Hendelse,
    val utfortAv: UtfortAv?,
    val oppgave: Oppgave,
) {
    fun erEndret() = hendelse.hendelsestype == Hendelsestype.OPPGAVE_ENDRET

    fun tilDomene() =
        OppdatertOppgaveHendelse(
            gsakOppgaveId = oppgave.oppgaveId,
            tilordnetSaksbehandler = oppgave.tilordning.navIdent,
        )
}

class Hendelse(
    val hendelsestype: Hendelsestype,
    val tidspunkt: LocalDateTime,
)

data class UtfortAv(
    val navIdent: String?,
    val enhetsnr: String?,
)

data class Oppgave(
    val oppgaveId: Long,
    val versjon: Int,
    val tilordning: Tilordning,
    val kategorisering: Kategorisering,
    val behandlingsperiode: Behandlingsperiode,
    val bruker: Bruker?,
)

data class Tilordning(
    val enhetsnr: String,
    val enhetsmappeId: Long?,
    val navIdent: String?,
)

data class Kategorisering(
    val tema: String,
    val oppgavetype: String,
    val behandlingstema: String?,
    val behandlingstype: String?,
    val prioritet: Prioritet,
)

enum class Prioritet {
    HOY,
    NORMAL,
    LAV,
}

data class Behandlingsperiode(
    val aktiv: LocalDate,
    val frist: LocalDate?,
)

data class Bruker(
    val ident: String,
    val identType: IdentType,
) {
    enum class IdentType {
        FOLKEREGISTERIDENT,
        NPID,
        ORGNR,
        SAMHANDLERNR,
    }
}

enum class Hendelsestype {
    OPPGAVE_OPPRETTET,
    OPPGAVE_ENDRET,
    OPPGAVE_FERDIGSTILT,
    OPPGAVE_FEILREGISTRERT,
}
