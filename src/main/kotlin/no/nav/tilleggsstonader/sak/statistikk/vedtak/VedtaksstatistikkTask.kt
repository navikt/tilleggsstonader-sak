package no.nav.tilleggsstonader.sak.statistikk.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.*
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = VedtaksstatistikkTask.TYPE,
    beskrivelse = "Lagrer vedtaksstatistikk i vedtaksstatistikk-tabell",
)
class VedtaksstatistikkTask(
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, fagsakId, hendelseTidspunkt) = objectMapper.readValue<VedtaksstatistikkTaskPayload>(task.payload)

        vedtaksstatistikkService.lagreVedtaksstatistikkV2(
            behandlingId,
            fagsakId,
        )

        vedtaksstatistikkService.lagreVedtaksstatistikk(
            behandlingId,
            fagsakId,
            hendelseTidspunkt,
        )
    }

    companion object {

        fun opprettVedtaksstatistikkTask(
            behandlingId: BehandlingId,
            fagsakId: FagsakId,
            hendelseTidspunkt: LocalDateTime = osloNow(),
            stønadstype: Stønadstype,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    VedtaksstatistikkTaskPayload(
                        behandlingId = behandlingId,
                        fagsakId = fagsakId,
                        hendelseTidspunkt = hendelseTidspunkt,
                    ),
                ),
                properties = Properties().apply {
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
        val hendelseTidspunkt: LocalDateTime,
    )
}
