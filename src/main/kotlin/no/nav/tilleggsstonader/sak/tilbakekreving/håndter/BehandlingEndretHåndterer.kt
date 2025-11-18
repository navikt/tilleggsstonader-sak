package no.nav.tilleggsstonader.sak.tilbakekreving.håndter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.tilbakekreving.TilbakekrevinghendelseService
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TILBAKEKREVING_TYPE_BEHANDLING_ENDRET
import no.nav.tilleggsstonader.sak.tilbakekreving.hendelse.TilbakekrevingBehandlingEndret
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BehandlingEndretHåndterer(
    private val oppgaveService: OppgaveService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilbakekrevinghendelseService: TilbakekrevinghendelseService,
    private val unleashService: UnleashService,
) : TilbakekrevingHendelseHåndterer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun håndtererHendelsetype(): String = TILBAKEKREVING_TYPE_BEHANDLING_ENDRET

    @Transactional
    override fun håndter(
        hendelseKey: String,
        payload: JsonNode,
    ) {
        val behandlingEndret = objectMapper.treeToValue<TilbakekrevingBehandlingEndret>(payload)

        if (gjelderTestsak(behandlingEndret)) {
            logger.debug(
                "Mottatt hendelse $TILBAKEKREVING_TYPE_BEHANDLING_ENDRET med ugyldig eksternFagsakId=${behandlingEndret.eksternFagsakId}, ignorerer melding",
            )
        } else {
            behandleBehandlingEndretHendelse(behandlingEndret)
        }
    }

    private fun behandleBehandlingEndretHendelse(behandlingEndret: TilbakekrevingBehandlingEndret) {
        val fagsak = fagsakService.hentFagsakPåEksternId(behandlingEndret.eksternFagsakId.toLong())
        val behandling = behandlingService.hentBehandlingPåEksternId(behandlingEndret.eksternBehandlingId.toLong())

        if (fagsak.id != behandling.fagsakId) {
            error("Fagsak har id ${fagsak.id} men behandling ${behandling.id} har fagsakId ${behandling.fagsakId}")
        }

        if (behandlingEndret.harStatusTilBehandling()) {
            opprettOppgaveForTilbakekrevingBehandling(behandlingEndret, fagsak, behandling)
        } else {
            logger.info(
                "Ignorerer hendelse ${behandlingEndret.hendelsestype} for tilbakekrevingsbehandling " +
                    "${behandlingEndret.tilbakekreving.behandlingId} med status ${behandlingEndret.tilbakekreving.behandlingsstatus}",
            )
        }

        val tilbakekrevingStatus = behandlingEndret.tilDomene()
        if (!tilbakekrevinghendelseService.harMottattHendelseMedStatus(behandling.id, tilbakekrevingStatus.behandlingstatus)) {
            tilbakekrevinghendelseService.persisterHendelse(behandling.id, tilbakekrevingStatus)
        }
    }

    private fun opprettOppgaveForTilbakekrevingBehandling(
        behandlingEndret: TilbakekrevingBehandlingEndret,
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        if (!unleashService.isEnabled(Toggle.OPPRETT_OPPGAVE_TILBAKEKREVING)) {
            logger.info("Oppretter ikke oppgave for tilbakekreving da toggle ${Toggle.OPPRETT_OPPGAVE_TILBAKEKREVING} er av")
            return
        }

        if (!finnesOppgaveForTilbakekreving(behandling.id)) {
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
                        beskrivelse =
                            "Tilbakekrevingssak behandles i nytt tilbakekrevingssystem " +
                                behandlingEndret.tilbakekreving.saksbehandlingURL,
                        prioritet = OppgavePrioritet.NORM,
                        fristFerdigstillelse = LocalDate.now().plusWeeks(2),
                        journalpostId = null,
                        opprettIMappe = OppgaveMappe.TILBAKEKREVING,
                    ),
            )
        } else {
            logger.info(
                "Oppgave for tilbakekrevingsbehandling ${behandlingEndret.tilbakekreving.behandlingId} finnes allerede, oppretter ikke ny oppgave",
            )
        }
    }

    private fun finnesOppgaveForTilbakekreving(behandlingId: BehandlingId): Boolean =
        oppgaveService
            .finnAlleOppgaveDomainForBehandling(behandlingId)
            .filter { it.erÅpen() }
            .any { it.tilbakekrevingBehandlingId != null }
}
