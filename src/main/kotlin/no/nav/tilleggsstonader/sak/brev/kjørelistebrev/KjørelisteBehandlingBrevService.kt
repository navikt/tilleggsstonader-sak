package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.finnSatserBruktIBeregning
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
    fun genererOgLagreBrev(
        behandlingId: BehandlingId,
        genererKjørelistebrevDto: GenererKjørelistebrevDto,
    ): KjørelisteBehandlingBrev {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        saksbehandling.status.validerKanBehandlingRedigeres()

        val begrunnelse = utledBegrunnelse(genererKjørelistebrevDto.begrunnelse, behandlingId)

        val html = genererHtml(saksbehandling, begrunnelse)
        val pdf = familieDokumentClient.genererPdf(html)

        return lagreEllerOppdaterBrev(saksbehandling, html, pdf, begrunnelse)
    }

    fun hentBegrunnelse(behandlingId: BehandlingId): String? =
        kjørelisteBehandlingBrevRepository.findByBehandlingId(behandlingId)?.begrunnelse

    fun utledBegrunnelse(
        nyBegrunnelse: String?,
        behandlingId: BehandlingId,
    ): String? =
        when {
            nyBegrunnelse == null -> hentBegrunnelse(behandlingId)
            nyBegrunnelse.isBlank() -> null
            else -> nyBegrunnelse
        }

    fun hentBrev(behandlingId: BehandlingId): KjørelisteBehandlingBrev {
        val brev = kjørelisteBehandlingBrevRepository.findByBehandlingId(behandlingId)

        brukerfeilHvis(brev == null) {
            "Finner ikke kjørelistebrev for behandlingId=$behandlingId. Det er nok fordi et brev ikke har blitt laget enda."
        }

        return brev
    }

    private fun genererHtml(
        saksbehandling: Saksbehandling,
        begrunnelse: String?,
    ): String {
        val vedtaksdata = vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(saksbehandling.id).data
        val beregningsresultatPrivatBil = vedtaksdata.beregningsresultat.privatBil
        val rammevedtak = vedtaksdata.rammevedtakPrivatBil

        brukerfeilHvis(beregningsresultatPrivatBil == null || rammevedtak == null) {
            "Finner ikke beregningsresultat for privat bil for behandling ${saksbehandling.id}"
        }

        val saksbehandlersignatur =
            if (saksbehandling.behandlingMetode == BehandlingMetode.AUTOMATISK) {
                null
            } else {
                SikkerhetContext.hentSaksbehandlerNavn(strict = true)
            }

        val oppsummertBeregning = oppsummerBeregningPrivatBil(beregningsresultatPrivatBil, rammevedtak)
        val request =
            KjørelisteBehandlingBrevRequest(
                navn = personService.hentVisningsnavnForPerson(saksbehandling.ident),
                ident = saksbehandling.ident,
                behandletDato = LocalDate.now(),
                saksbehandlerSignatur = saksbehandlersignatur,
                behandlendeEnhet = utledBehandlendeEnhet(saksbehandling.stønadstype),
                beregning = oppsummertBeregning,
                satser = oppsummertBeregning.finnSatserBruktIBeregning(),
                begrunnelse = begrunnelse,
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
        begrunnelse: String?,
    ): KjørelisteBehandlingBrev {
        val brev =
            KjørelisteBehandlingBrev(
                behandlingId = saksbehandling.id,
                saksbehandlerHtml = html,
                pdf = Fil(pdf),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                begrunnelse = begrunnelse,
            )

        if (kjørelisteBehandlingBrevRepository.existsById(saksbehandling.id)) {
            kjørelisteBehandlingBrevRepository.update(brev)
        } else {
            kjørelisteBehandlingBrevRepository.insert(brev)
        }

        return brev
    }
}
