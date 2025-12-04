package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = VedtaksstatistikkTask.TYPE,
    beskrivelse = "Lagrer vedtaksstatistikk i vedtaksstatistikk-tabell",
)
class VedtaksstatistikkTask(
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, fagsakId) = jsonMapper.readValue<VedtaksstatistikkTaskPayload>(task.payload)

        vedtaksstatistikkService.lagreVedtaksstatistikkV2(
            behandlingId,
            fagsakId,
        )
    }

    companion object {
        fun opprettVedtaksstatistikkTask(
            behandlingId: BehandlingId,
            fagsakId: FagsakId,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            stønadstype: Stønadstype,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        VedtaksstatistikkTaskPayload(
                            behandlingId = behandlingId,
                            fagsakId = fagsakId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["behandlingId"] = behandlingId.toString()
                        this["fagsakId"] = fagsakId.toString()
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                        this["stønadstype"] = stønadstype.toString()
                    },
            )

        const val TYPE = "vedtaksstatistikkTask"
    }

    data class VedtaksstatistikkTaskPayload(
        val behandlingId: BehandlingId,
        val fagsakId: FagsakId,
    )
}
