package no.nav.tilleggsstonader.sak.statistikk.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingKafkaProducer
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingsstatistikkService
import org.springframework.stereotype.Service
import java.time.ZoneId

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til DVH",
)
class BehandlingsstatistikkTask(
    private val behandlingService: BehandlingService,
    private val behandlingsstatistikkService: BehandlingsstatistikkService,
    private val behandlingKafkaProducer : BehandlingKafkaProducer

    ): AsyncTaskStep {

    private val zoneIdOslo = ZoneId.of("Europe/Oslo")

    override fun doTask(task: Task) {
      val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode) =
                  objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

             val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
             // val årsakRevurdering = årsakRevurderingService.hentÅrsakRevurdering(behandlingId)

              val sisteOppgaveForBehandling = finnSisteOppgaveForBehandlingen(behandlingId, oppgaveId)
              val vedtak = vedtakRepository.findByIdOrNull(behandlingId)

              val resultatBegrunnelse = finnResultatBegrunnelse(hendelse, vedtak, saksbehandling)
              val søker = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.søker
            val henvendelseTidspunkt = finnHenvendelsestidspunkt(saksbehandling)
              val relatertEksternBehandlingId =
                  saksbehandling.forrigeBehandlingId?.let { behandlingService.hentBehandling(it).eksternId.id }
              val erAutomatiskGOmregning = saksbehandling.årsak == BehandlingÅrsak.G_OMREGNING && saksbehandling.opprettetAv == "VL"

              val behandlingsstatistikkDto = BehandlingsstatistikkDto(
                  behandlingId = behandlingId,
                  eksternBehandlingId = saksbehandling.eksternId,
                  personIdent = saksbehandling.ident,
                  gjeldendeSaksbehandlerId = if (erAutomatiskGOmregning) {
                      "VL"
                  } else {
                      finnSaksbehandler(hendelse, vedtak, gjeldendeSaksbehandler)
                  },
                  beslutterId = if (hendelse.erBesluttetEllerFerdig()) {
                      vedtak?.beslutterIdent

                  behandlingKafkaProducer.sendBehandling(behandlingsstatistikkDto)
                  }

    }