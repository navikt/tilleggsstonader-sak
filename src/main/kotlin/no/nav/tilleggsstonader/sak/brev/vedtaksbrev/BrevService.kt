package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BREVDATO_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.SAKSBEHANDLER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.norskFormat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class BrevService(
    private val vedtaksbrevRepository: VedtaksbrevRepository,
    private val familieDokumentClient: FamilieDokumentClient,
    private val tilgangService: TilgangService,
) {
    fun lagSaksbehandlerBrev(
        saksbehandling: Saksbehandling,
        html: String,
    ): ByteArray {
        validerRedigerbarBehandling(saksbehandling)

        val saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val htmlMedSignatur = settInnSaksbehandlerSignatur(html, saksbehandlersignatur)

        lagreEllerOppdaterSaksbehandlerVedtaksbrev(
            behandlingId = saksbehandling.id,
            saksbehandlersignatur = saksbehandlersignatur,
            saksbehandlerHtml = htmlMedSignatur,
        )

        return familieDokumentClient.genererPdf(htmlMedSignatur)
    }

    fun hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId: BehandlingId): ByteArray {
        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandlingId)
        return when (vedtaksbrev.beslutterPdf) {
            null -> familieDokumentClient.genererPdf(vedtaksbrev.saksbehandlerHtml)
            else -> vedtaksbrev.beslutterPdf.bytes
        }
    }

    private fun lagreEllerOppdaterSaksbehandlerVedtaksbrev(
        behandlingId: BehandlingId,
        saksbehandlersignatur: String,
        saksbehandlerHtml: String,
    ): Vedtaksbrev {
        val vedtaksbrev =
            Vedtaksbrev(
                behandlingId = behandlingId,
                saksbehandlerHtml = saksbehandlerHtml,
                saksbehandlersignatur = saksbehandlersignatur,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(),
            )

        return when (vedtaksbrevRepository.existsById(behandlingId)) {
            true -> vedtaksbrevRepository.update(vedtaksbrev)
            false -> vedtaksbrevRepository.insert(vedtaksbrev)
        }
    }

    fun forhåndsvisBeslutterBrev(saksbehandling: Saksbehandling): ByteArray {
        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(saksbehandling.id)

        val beslutterSignatur =
            if (tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER)) {
                SikkerhetContext.hentSaksbehandlerNavn(strict = true)
            } else {
                ""
            }

        return lagBeslutterPdfMedSignatur(vedtaksbrev.saksbehandlerHtml, beslutterSignatur).bytes
    }

    fun lagEndeligBeslutterbrev(saksbehandling: Saksbehandling): Fil {
        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(saksbehandling.id)
        val saksbehandlerHtml = hentSaksbehandlerHtml(vedtaksbrev, saksbehandling)
        val beslutterIdent = SikkerhetContext.hentSaksbehandler()
        val beslutterSignatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        validerKanLageBeslutterbrev(saksbehandling, vedtaksbrev)
        val beslutterPdf = lagBeslutterPdfMedSignatur(saksbehandlerHtml, beslutterSignatur)
        val besluttervedtaksbrev =
            vedtaksbrev.copy(
                besluttersignatur = beslutterSignatur,
                beslutterIdent = beslutterIdent,
                beslutterPdf = beslutterPdf,
                besluttetTid = osloNow(),
            )
        vedtaksbrevRepository.update(besluttervedtaksbrev)
        return Fil(bytes = beslutterPdf.bytes)
    }

    private fun hentSaksbehandlerHtml(
        vedtaksbrev: Vedtaksbrev,
        saksbehandling: Saksbehandling,
    ): String {
        feilHvis(vedtaksbrev.saksbehandlerHtml.isEmpty()) {
            "Mangler innhold i saksbehandlerbrev for behandling: ${saksbehandling.id}"
        }
        return vedtaksbrev.saksbehandlerHtml
    }

    private fun validerKanLageBeslutterbrev(
        behandling: Saksbehandling,
        vedtaksbrev: Vedtaksbrev,
    ) {
        if (behandling.steg != StegType.BESLUTTE_VEDTAK || behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil(
                "Behandling er i feil steg=${behandling.steg} status=${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        feilHvisIkke(vedtaksbrev.beslutterPdf == null) {
            "Det finnes allerede et beslutterbrev"
        }
    }

    private fun lagBeslutterPdfMedSignatur(
        saksbehandlerHtml: String,
        beslutterSignatur: String,
    ): Fil {
        val htmlMedBeslutterSignatur =
            settInnBeslutterSignaturOgVedtaksdato(
                html = saksbehandlerHtml,
                beslutterSignatur = beslutterSignatur,
            )
        return Fil(familieDokumentClient.genererPdf(htmlMedBeslutterSignatur))
    }

    private fun settInnSaksbehandlerSignatur(
        html: String,
        saksbehandlerSignatur: String,
    ): String {
        feilHvisIkke(html.contains(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for saksbehandlersignatur"
        }

        return html.replace(SAKSBEHANDLER_SIGNATUR_PLACEHOLDER, saksbehandlerSignatur)
    }

    private fun settInnBeslutterSignaturOgVedtaksdato(
        html: String,
        beslutterSignatur: String,
    ): String {
        feilHvis(!html.contains(BESLUTTER_SIGNATUR_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for besluttersignatur"
        }

        feilHvis(!html.contains(BREVDATO_PLACEHOLDER)) {
            "Brev-HTML mangler placeholder for vedtaksdato"
        }

        return html
            .replace(BESLUTTER_SIGNATUR_PLACEHOLDER, beslutterSignatur)
            .replace(BREVDATO_PLACEHOLDER, osloDateNow().norskFormat())
    }

    private fun validerRedigerbarBehandling(saksbehandling: Saksbehandling) {
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandling er i feil steg=${saksbehandling.steg} status=${saksbehandling.status}"
        }
    }

    fun slettVedtaksbrev(saksbehandling: Saksbehandling) {
        vedtaksbrevRepository.deleteById(saksbehandling.id)
    }

    fun hentBesluttetBrev(behandlingId: BehandlingId): Vedtaksbrev {
        val vedtaksbrev = vedtaksbrevRepository.findByIdOrThrow(behandlingId)

        feilHvis(vedtaksbrev.beslutterPdf == null) {
            "Fant ikke besluttet pdf"
        }

        return vedtaksbrev
    }
}
