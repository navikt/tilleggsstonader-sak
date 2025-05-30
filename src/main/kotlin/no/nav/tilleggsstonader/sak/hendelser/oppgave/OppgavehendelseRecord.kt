package no.nav.tilleggsstonader.sak.hendelser.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppdatertOppgaveHendelse
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgavehendelseRecord(
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

@JsonIgnoreProperties(ignoreUnknown = true)
class Hendelse(
    val hendelsestype: Hendelsestype,
    val tidspunkt: LocalDateTime,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtfortAv(
    val navIdent: String?,
    val enhetsnr: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Oppgave(
    val oppgaveId: Long,
    val versjon: Int,
    val tilordning: Tilordning,
    val kategorisering: Kategorisering,
    val behandlingsperiode: Behandlingsperiode,
    val bruker: Bruker?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tilordning(
    val enhetsnr: String,
    val enhetsmappeId: Long?,
    val navIdent: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class Behandlingsperiode(
    val aktiv: LocalDate,
    val frist: LocalDate?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
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
