package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.kontrakter.journalpost.Dokumentvariantformat
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.søknad.InnsendtSkjema
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandling
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingOppgaveMetadata
import no.nav.tilleggsstonader.sak.behandling.OpprettBehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.tilleggsstonader.sak.journalføring.JournalføringHelper
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.journalføring.dokumentBrevkode
import no.nav.tilleggsstonader.sak.opplysninger.kjøreliste.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.opplysninger.kjøreliste.KjørelisteDag
import no.nav.tilleggsstonader.sak.opplysninger.kjøreliste.KjørelisteService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
class HåndterMottattKjørelisteService(
    private val journalpostClient: JournalpostClient,
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val behandlingService: BehandlingService,
    private val journalpostService: JournalpostService,
    private val fagsakService: FagsakService,
    private val kjørelisteService: KjørelisteService,
    private val opprettBehandlingService: OpprettBehandlingService,
) {
    fun behandleKjøreliste(journalpost: Journalpost) {
        val kjørelisteSkjema =
            hentKjørelisteSkjemaFraJournalpost(journalpost)
        val reiseId = ReiseId(UUID.fromString(kjørelisteSkjema.reiseId))

        val ident = journalpost.bruker?.id ?: error("Ingen bruker tilknyttet journalpost for kjøreliste")
        val rammevedtakPrivatBil = dagligReisePrivatBilService.hentRammevedtakPåIdent(ident)

        val rammevedtakTilhørendeKjøreliste =
            rammevedtakPrivatBil.firstOrNull { rammevedtak ->
                rammevedtak.data.rammevedtakPrivatBil
                    ?.reiser
                    ?.map { reise -> reise.reiseId }
                    ?.contains(reiseId) == true
            } ?: error("Finner ingen rammevedtak med reiseId $reiseId for innsendt kjøreliste")

        val saksbehandling = behandlingService.hentSaksbehandling(rammevedtakTilhørendeKjøreliste.behandlingId)
        val fagsak = fagsakService.hentFagsak(saksbehandling.fagsakId)

        // TODO - lagre kjøreliste, opprette behandling
        lagreKjøreliste(kjørelisteSkjema, reiseId, fagsak, journalpost.journalpostId)

        val behandling =
            opprettBehandlingService.opprettBehandling(
                OpprettBehandling(
                    fagsakId = fagsak.id,
                    behandlingsårsak = BehandlingÅrsak.KJØRELISTE,
                    kravMottatt = journalpost.datoMottatt?.toLocalDate(),
                    oppgaveMetadata = OpprettBehandlingOppgaveMetadata.UtenOppgave,
                ),
            )

        // TODO -

        journalpostService.oppdaterOgFerdigstillJournalpost(
            journalpost = journalpost,
            dokumenttitler = null,
            logiskeVedlegg = null,
            journalførendeEnhet = MASKINELL_JOURNALFOERENDE_ENHET,
            fagsak = fagsak,
            saksbehandler = SYSTEM_FORKORTELSE,
            avsender = null,
        )
    }

    private fun lagreKjøreliste(
        skjema: KjørelisteSkjema,
        reiseId: ReiseId,
        fagsak: Fagsak,
        journalpostId: String,
    ) {
        val kjørelisteDomene =
            InnsendtKjøreliste(
                reiseId = reiseId,
                dagerKjørt =
                    skjema.reisedagerPerUkeAvsnitt.flatMap { ukeMedReisedager ->
                        ukeMedReisedager.reisedager.map { reisedag ->
                            KjørelisteDag(
                                dato = reisedag.dato.verdi,
                                parkeringsutgift = reisedag.parkeringsutgift.verdi?.toInt(),
                            )
                        }
                    },
            )

        kjørelisteService.lagre(kjørelisteDomene, fagsak.id, journalpostId)
    }

    private fun hentKjørelisteSkjemaFraJournalpost(journalpost: Journalpost): KjørelisteSkjema {
        val dokumentBrevkode = journalpost.dokumentBrevkode()
        feilHvis(dokumentBrevkode == null) {
            "Finner ikke dokumentBrevkode for journalpost=${journalpost.journalpostId}"
        }
        val dokumentInfo = JournalføringHelper.plukkUtOriginaldokument(journalpost, dokumentBrevkode)
        val kjørelisteFraJournalpost =
            journalpostClient.hentDokument(
                journalpostId = journalpost.journalpostId,
                dokumentInfoId = dokumentInfo.dokumentInfoId,
                Dokumentvariantformat.ORIGINAL,
            )

        return jsonMapper.readValue<InnsendtSkjema<KjørelisteSkjema>>(kjørelisteFraJournalpost).skjema
    }
}
