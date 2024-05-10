package no.nav.tilleggsstonader.sak.statistikk.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.osloNow
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

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

        vedtaksstatistikkService.lagreVedtaksstatistikk(
            behandlingId,
            fagsakId,
            hendelseTidspunkt,
        )
    }

    companion object {

        fun opprettVedtaksstatistikkTask(
            behandlingId: UUID,
            fagsakId: UUID,
            hendelseTidspunkt: LocalDateTime = osloNow(),
            stønadstype: Stønadstype = Stønadstype.BARNETILSYN,
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
        val behandlingId: UUID,
        val fagsakId: UUID,
        val hendelseTidspunkt: LocalDateTime,
    )
}
