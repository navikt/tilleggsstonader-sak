package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingBehandlingEndret
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BehandlingEndretHåndterer(
    private val oppgaveService: OppgaveService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
) : TilbakekrevingHendelseHåndterer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun håndtererHendelsetype(): String = "behandling_endret"

    override fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    ) {
        val behandlingEndretHendelse = objectMapper.treeToValue<TilbakekrevingBehandlingEndret>(payload)
        if (behandlingEndretHendelse.harStatusTilBehandling()) {
            håndterHendelseTilBehandling(behandlingEndretHendelse)
        } else {
            logger.info(
                "Ignorerer hendelse ${behandlingEndretHendelse.hendelsestype} for tilbakekrevingsbehandling " +
                    "${behandlingEndretHendelse.tilbakekreving.behandlingId} med status ${behandlingEndretHendelse.tilbakekreving.behandlingsstatus}",
            )
        }
    }

    private fun håndterHendelseTilBehandling(behandlingEndret: TilbakekrevingBehandlingEndret) {
        val fagsak = fagsakService.hentFagsakPåEksternId(behandlingEndret.eksternFagsakId.toLong())
        val behandling = behandlingService.hentBehandlingPåEksternId(behandlingEndret.eksternBehandlingId.toLong())

        if (fagsak.id != behandling.fagsakId) {
            error("Fagsak har id ${fagsak.id} men behandling ${behandling.id} har fagsakId ${behandling.fagsakId}")
        }

        if (finnesOppgaveForTilbakekreving(behandling.id)) {
            logger.info(
                "Oppgave for tilbakekrevingsbehandling ${behandlingEndret.tilbakekreving.behandlingId} finnes allerede, oppretter ikke ny oppgave",
            )
        } else {
            logger.info(
                "Oppretter oppgave for tilbakekrevingsbehandling ${behandlingEndret.tilbakekreving.behandlingId} som er satt til TIL_BEHANDLING",
            )
            oppgaveService.opprettOppgave(
                personIdent = fagsak.hentAktivIdent(),
                stønadstype = fagsak.stønadstype,
                behandlingId = behandling.id,
                tilbakekrevingBehandlingId = behandlingEndret.tilbakekreving.behandlingId,
                oppgave =
                    OpprettOppgave(
                        oppgavetype = Oppgavetype.BehandleSak,
                        beskrivelse = "Tilbakekrevingssak behandles i nytt tilbakekrevingssystem ${behandlingEndret.tilbakekreving.saksbehandlingURL}",
                        prioritet = OppgavePrioritet.NORM,
                        fristFerdigstillelse = LocalDate.now().plusWeeks(2),
                        journalpostId = null,
                        opprettIMappe = OppgaveMappe.TILBAKEKREVING,
                    ),
            )
        }
    }

    private fun finnesOppgaveForTilbakekreving(behandlingId: BehandlingId): Boolean =
        oppgaveService.finnAlleOppgaveDomainForBehandling(behandlingId).any {
            it.tilbakekrevingBehandlingId != null
        }
}
