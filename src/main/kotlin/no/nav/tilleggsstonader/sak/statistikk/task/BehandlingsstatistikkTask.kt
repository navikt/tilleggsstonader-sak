package no.nav.tilleggsstonader.sak.statistikk.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingsstatistikkService
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingsstatistikkDto
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

// TODO refactorer mappingkode fra Task til BehandlingsstatistikkService
@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til DVH",
)
class BehandlingsstatistikkTask(
    private val behandlingService: BehandlingService,
    private val behandlingsstatistikkService: BehandlingsstatistikkService,
    private val oppgaveService: OppgaveService,
    private val personService: PersonService,
    private val totrinnskontrollService: TotrinnskontrollService,

) : AsyncTaskStep {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode) =
            objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
        val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling)

        val relatertEksternBehandlingId = saksbehandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it).forrigeBehandlingId }
        val behandlingsstatistikkDto = BehandlingsstatistikkDto(
            behandlingId = behandlingId,
            eksternBehandlingId = saksbehandling.eksternId,
            personIdent = saksbehandling.ident,
            gjeldendeSaksbehandlerId =
            finnSaksbehandler(hendelse, totrinnskontrollService, gjeldendeSaksbehandler, behandlingId),
            beslutterId = if (hendelse.erBesluttetEllerFerdig()) {
                totrinnskontrollService.hentBeslutter(behandlingId)
            } else {
                null
            },
            eksternFagsakId = saksbehandling.eksternFagsakId,
            hendelseTidspunkt = hendelseTidspunkt.atZone(zoneIdOslo),
            behandlingOpprettetTidspunkt = saksbehandling.opprettetTid.atZone(zoneIdOslo),
            hendelse = hendelse,
            behandlingResultat = saksbehandling.resultat.name,
            resultatBegrunnelse = null, // TODO oppdater når dette feltet er blitt enum som kan raporteres .resultatBegrunnelse,
            opprettetEnhet = sisteOppgaveForBehandling?.opprettetAvEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
            ansvarligEnhet = sisteOppgaveForBehandling?.tildeltEnhetsnr ?: MASKINELL_JOURNALFOERENDE_ENHET,
            strengtFortroligAdresse = evalerAdresseBeskyttelseStrengtFortrolig(saksbehandling.ident),
            stønadstype = saksbehandling.stønadstype,
            behandlingstype = BehandlingType.valueOf(saksbehandling.type.name),
            henvendelseTidspunkt = henvendelseTidspunkt.atZone(zoneIdOslo),
            relatertEksternBehandlingId = null, // relatertEksternBehandlingId,
            relatertBehandlingId = null,
            behandlingMetode = behandlingMetode,
            behandlingÅrsak = saksbehandling.årsak,
            // kravMottatt = saksbehandling.kravMottatt,
            årsakRevurdering = null, // årsakRevurdering?.let {
            // ÅrsakRevurderingDto(it.opplysningskilde, it.årsak)
            // },
            // avslagÅrsak = vedtak?.avslåÅrsak,
            kategori = saksbehandling.kategori,
        )
        behandlingsstatistikkService.sendBehandlingstatistikk(behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode)
    }
    private fun finnSisteOppgaveForBehandlingen(behandlingId: UUID, oppgaveId: Long?): Oppgave? {
        val gsakOppgaveId = oppgaveId ?: oppgaveService.finnSisteOppgaveForBehandling(behandlingId)?.gsakOppgaveId

        return gsakOppgaveId?.let { oppgaveService.hentOppgave(it) }
    }

    private fun Hendelse.erBesluttetEllerFerdig() = this.name == Hendelse.BESLUTTET.name || this.name == Hendelse.FERDIG.name

    private fun finnSaksbehandler(hendelse: Hendelse, totrinnskontrollService: TotrinnskontrollService, gjeldendeSaksbehandler: String?, behandlingId: UUID): String {
        return when (hendelse) {
            Hendelse.MOTTATT, Hendelse.PÅBEGYNT, Hendelse.VENTER, Hendelse.HENLAGT ->
                gjeldendeSaksbehandler ?: error("Mangler saksbehandler for hendelse")
            Hendelse.VEDTATT, Hendelse.BESLUTTET, Hendelse.FERDIG -> totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId)
        }
    }

    private fun finnHenvendelsestidspunkt(saksbehandling: Saksbehandling): LocalDateTime {
        return when (saksbehandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> saksbehandling.opprettetTid
            BehandlingType.REVURDERING -> saksbehandling.opprettetTid
        }
    }
    private fun evalerAdresseBeskyttelseStrengtFortrolig(personIdent: String): Boolean {
        val adresseStatus = personService.hentPersonKortBolk(listOf(personIdent)).values.single().adressebeskyttelse.gradering()
        return when (adresseStatus) {
            AdressebeskyttelseGradering.STRENGT_FORTROLIG, AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> true
            AdressebeskyttelseGradering.FORTROLIG, AdressebeskyttelseGradering.UGRADERT -> false
        }
    }

    companion object {

        fun opprettMottattTask(
            behandlingId: UUID,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            saksbehandler: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            oppgaveId: Long?,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = saksbehandler,
                oppgaveId = oppgaveId,
            )

        fun opprettPåbegyntTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
            )

        fun opprettVenterTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VENTER,
                hendelseTidspunkt = LocalDateTime.now(),
                gjeldendeSaksbehandler = SikkerhetContext.hentSaksbehandler(),
            )

        fun opprettVedtattTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VEDTATT,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettBesluttetTask(
            behandlingId: UUID,
            oppgaveId: Long?,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.BESLUTTET,
                hendelseTidspunkt = LocalDateTime.now(),
                oppgaveId = oppgaveId,
            )

        fun opprettFerdigTask(behandlingId: UUID): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettHenlagtTask(behandlingId: UUID, hendelseTidspunkt: LocalDateTime, gjeldendeSaksbehandler: String): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = hendelseTidspunkt,
                gjeldendeSaksbehandler = gjeldendeSaksbehandler,
            )

        private fun opprettTask(
            behandlingId: UUID,
            hendelse: Hendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String? = null,
            oppgaveId: Long? = null,
            behandlingMetode: BehandlingMetode? = null,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    BehandlingsstatistikkTaskPayload(
                        behandlingId,
                        hendelse,
                        hendelseTidspunkt,
                        gjeldendeSaksbehandler,
                        oppgaveId,
                        behandlingMetode,
                    ),
                ),
                properties = Properties().apply {
                    this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                    this["behandlingId"] = behandlingId.toString()
                    this["hendelse"] = hendelse.name
                    this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                    this["oppgaveId"] = oppgaveId?.toString() ?: ""
                },
            )

        const val TYPE = "behandlingsstatistikkTask"
    }
    data class BehandlingsstatistikkTaskPayload(
        val behandlingId: UUID,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String?,
        val oppgaveId: Long?,
        val behandlingMetode: BehandlingMetode?,
    )
}
