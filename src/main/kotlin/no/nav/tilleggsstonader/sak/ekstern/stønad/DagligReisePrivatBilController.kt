package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.sikkerhet.EksternBrukerUtils
import no.nav.tilleggsstonader.libs.utils.dato.ukenummer
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakUkeDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping(
    path = ["/api/ekstern/privat-bil"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class DagligReisePrivatBilController(
    private val dagligReisePrivatBilService: DagligReisePrivatBilService,
    private val avklartKjørelisteService: AvklartKjørelisteService,
) {
    @GetMapping("/rammevedtak")
    @ProtectedWithClaims(issuer = EksternBrukerUtils.ISSUER_TOKENX, claimMap = ["acr=Level4"])
    fun hentRammevedtak(): List<RammevedtakDto> {
        val ident = EksternBrukerUtils.hentFnrFraToken()
        val vedtakListe = dagligReisePrivatBilService.hentRammevedtakPåIdent(ident)
        return vedtakListe.flatMap { generiskVedtak ->
            val behandlingId = generiskVedtak.behandlingId
            val avklarteUker = avklartKjørelisteService.hentAvklarteUkerForBehandling(behandlingId)
            generiskVedtak.data.rammevedtakPrivatBil?.tilDto(avklarteUker) ?: emptyList()
        }
    }
}

private fun RammevedtakPrivatBil.tilDto(avklarteUker: List<AvklartKjørtUke>): List<RammevedtakDto> {
    val dagensUke = LocalDate.now().ukenummer()
    return reiser.map { reise ->
        RammevedtakDto(
            reiseId = reise.reiseId,
            fom = reise.grunnlag.fom,
            tom = reise.grunnlag.tom,
            reisedagerPerUke = reise.grunnlag.reisedagerPerUke,
            aktivitetsadresse = reise.aktivitetsadresse ?: "Ukjent adresse",
            aktivitetsnavn = "Ukjent aktivitet",
            uker =
                reise.uker.map { uke ->
                    val ukeNummer = uke.grunnlag.fom.ukenummer()
                    val avklartUke = avklarteUker.find { it.ukenummer == ukeNummer }
                    RammevedtakUkeDto(
                        fom = uke.grunnlag.fom,
                        tom = uke.grunnlag.tom,
                        ukeNummer = ukeNummer,
                        innsendtDato = avklartUke?.innsendtDato,
                        kanSendeInnKjøreliste = ukeNummer <= dagensUke && avklartUke?.innsendtDato == null,
                    )
                },
        )
    }
}
