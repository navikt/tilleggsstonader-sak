package no.nav.tilleggsstonader.sak.kjøreliste

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.behandling.OpprettRevurderingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.domain.OpprettRevurdering
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleMottattKjørelisteTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Behandler mottatt kjøreliste",
)
class BehandleMottattKjørelisteTask(
    private val opprettRevurderingService: OpprettRevurderingService,
    private val kjørelisteService: KjørelisteService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val kjøreliste = kjørelisteService.hentKjøreliste(UUID.fromString(task.payload))
        opprettRevurderingService.opprettRevurdering(
            OpprettRevurdering(
                fagsakId = kjøreliste.fagsakId,
                årsak = BehandlingÅrsak.KJØRELISTE,
                nyeOpplysningerMetadata = null,
                valgteBarn = emptySet(),
                kravMottatt = kjøreliste.datoMottatt.toLocalDate(),
                skalOppretteOppgave = true,
            ),
        )
    }

    companion object {
        const val TYPE = "behandleMottattKjørelisteTask"

        fun opprettTask(kjørelisteId: UUID): Task =
            Task(
                type = TYPE,
                payload = kjørelisteId.toString(),
                properties =
                    Properties().apply {
                        setProperty("kjørelisteId", kjørelisteId.toString())
                    },
            )
    }
}
