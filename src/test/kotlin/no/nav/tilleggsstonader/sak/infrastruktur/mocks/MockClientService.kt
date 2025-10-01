package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingClient
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.klage.KlageClient
import no.nav.tilleggsstonader.sak.klage.KlageClientMockConfig
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetClient
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattClient
import no.nav.tilleggsstonader.sak.opplysninger.fullmakt.FullmaktClient
import no.nav.tilleggsstonader.sak.opplysninger.kodeverk.KodeverkClient
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.opplysninger.tilordnetSaksbehandler.TilordnetSaksbehandlerClient
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import org.springframework.stereotype.Service

/**
 * Service for å enklere tilbakestille mocks i tester, og er eksponert i [no.nav.tilleggsstonader.sak.IntegrationTest]
 */
@Service
class MockClientService(
    val arbeidsfordelingClient: ArbeidsfordelingClient,
    val arenaClient: ArenaClient,
    val egenAnsattClient: EgenAnsattClient,
    val familieDokumentClient: FamilieDokumentClient,
    val fullmaktClient: FullmaktClient,
    val htmlifyClient: HtmlifyClient,
    val iverksettClient: IverksettClient,
    val journalpostClient: JournalpostClient,
    val klageClient: KlageClient,
    val kodeverkClient: KodeverkClient,
    val oppgaveClient: OppgaveClient,
    val oppgavelager: Oppgavelager,
    val pdlClientService: PdlClient,
    val registerAktivitetClient: RegisterAktivitetClient,
    val tilordnetSaksbehandlerClient: TilordnetSaksbehandlerClient,
    val ytelseClient: YtelseClient,
) {
    fun resetAlleTilDefaults() {
        ArbeidsfordelingClientMockConfig.resetTilDefault(arbeidsfordelingClient)
        ArenaClientMockConfig.resertTilDefault(arenaClient)
        EgenAnsattClientMockConfig.resetTilDefault(egenAnsattClient)
        FamilieDokumentClientMockConfig.resetTilDefault(familieDokumentClient)
        FullmaktClientMockConfig.resetTilDefault(fullmaktClient)
        HtmlifyClientMockConfig.resetTilDefault(htmlifyClient)
        IverksettClientMockConfig.resetTilDefault(iverksettClient)
        JournalpostClientMockConfig.resetTilDefault(journalpostClient)
        KlageClientMockConfig.resetTilDefault(klageClient)
        KodeverkClientMockConfig.resetTilDefault(kodeverkClient)
        OppgaveClientMockConfig.resetTilDefault(oppgaveClient, oppgavelager = oppgavelager)
        PdlClientMockConfig.resetTilDefault(pdlClientService)
        RegisterAktivitetClientMockConfig.resetTilDefault(registerAktivitetClient)
        TilordnetSaksbehandlerClientMockConfig.resetTilDefault(tilordnetSaksbehandlerClient)
        YtelseClientMockConfig.resetTilDefault(ytelseClient)
    }
}
