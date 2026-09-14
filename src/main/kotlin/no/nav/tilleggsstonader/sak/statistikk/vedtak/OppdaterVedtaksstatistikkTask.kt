package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterVedtaksstatistikkTask.TYPE,
    beskrivelse = "Oppdaterer vedtaksstatistikk for en behandling som allerede finnes i vedtaksstatistikk-tabellen",
)
class OppdaterVedtaksstatistikkTask(
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId) = jsonMapper.readValue<OppdaterVedtaksstatistikkTaskPayload>(task.payload)

        vedtaksstatistikkService.oppdaterVedtaksstatistikkV2(behandlingId)
    }

    companion object {
        fun opprettOppdaterVedtaksstatistikkTask(
            behandlingId: BehandlingId,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            stønadstype: Stønadstype,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        OppdaterVedtaksstatistikkTaskPayload(
                            behandlingId = behandlingId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                        this["stønadstype"] = stønadstype.toString()
                    },
            )

        const val TYPE = "oppdaterVedtaksstatistikkTask"
    }

    data class OppdaterVedtaksstatistikkTaskPayload(
        val behandlingId: BehandlingId,
    )
}
