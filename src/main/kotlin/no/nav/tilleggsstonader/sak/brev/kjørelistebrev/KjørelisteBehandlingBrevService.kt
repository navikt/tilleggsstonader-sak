package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.oppsummerBeregningPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KjørelisteBehandlingBrevService(
    private val kjørelisteBehandlingBrevRepository: KjørelisteBehandlingBrevRepository,
    private val familieDokumentClient: FamilieDokumentClient,
    private val htmlifyClient: HtmlifyClient,
    private val personService: PersonService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
) {
    fun genererOgLagreBrev(behandlingId: BehandlingId): ByteArray {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        saksbehandling.status.validerKanBehandlingRedigeres()

        val html = genererHtml(saksbehandling)
        val pdf = familieDokumentClient.genererPdf(html)

        lagreEllerOppdaterBrev(saksbehandling, html, pdf)

        return pdf
    }

    fun hentBrev(behandlingId: BehandlingId): ByteArray {
        val brev = kjørelisteBehandlingBrevRepository.findByBehandlingId(behandlingId)

        brukerfeilHvis(brev == null) {
            "Finner ikke kjørelistebrev for behandlingId=$behandlingId. Det er nok fordi et brev ikke har blitt laget enda."
        }

        return brev.pdf.bytes
    }

    private fun genererHtml(saksbehandling: Saksbehandling): String {
        val vedtaksdata = vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(saksbehandling.id).data
        val beregningsresultatPrivatBil = vedtaksdata.beregningsresultat.privatBil
        val rammevedtak = vedtaksdata.rammevedtakPrivatBil

        brukerfeilHvis(beregningsresultatPrivatBil == null || rammevedtak == null) {
            "Finner ikke beregningsresultat for privat bil for behandling ${saksbehandling.id}"
        }

        val saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val request =
            KjørelisteBehandlingBrevRequest(
                navn = personService.hentVisningsnavnForPerson(saksbehandling.ident),
                ident = saksbehandling.ident,
                behandletDato = LocalDate.now(),
                saksbehandlerSignatur = saksbehandlersignatur,
                behandlendeEnhet = utledBehandlendeEnhet(saksbehandling.stønadstype),
                beregning = oppsummerBeregningPrivatBil(beregningsresultatPrivatBil, rammevedtak),
            )

        return htmlifyClient.genererKjørelisteBehandlingBrev(request)
    }

    private fun utledBehandlendeEnhet(stønadstype: Stønadstype): String =
        when (stønadstype) {
            Stønadstype.DAGLIG_REISE_TSR -> "Nav Tiltak Oslo"
            Stønadstype.DAGLIG_REISE_TSO -> "Nav Arbeid og ytelser"
            else -> error("Uforventet stønadstype $stønadstype i en kjørelistebehandling")
        }

    private fun lagreEllerOppdaterBrev(
        saksbehandling: Saksbehandling,
        html: String,
        pdf: ByteArray,
    ) {
        val brev =
            KjørelisteBehandlingBrev(
                behandlingId = saksbehandling.id,
                saksbehandlerHtml = html,
                pdf = Fil(pdf),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(),
            )

        if (kjørelisteBehandlingBrevRepository.existsById(saksbehandling.id)) {
            kjørelisteBehandlingBrevRepository.update(brev)
        } else {
            kjørelisteBehandlingBrevRepository.insert(brev)
        }
    }
}
